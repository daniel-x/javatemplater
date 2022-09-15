package de.a0h.javatemplater;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.a0h.javatemplater.MethodSourceTemplate.Param;

/**
 * For generating source code from source templates, this class can capture
 * source code of annotated methods from .java files and turn the code into
 * Strings containing source code. Keep your template files well-formatted.
 * Otherwise it might not work.
 * 
 * <p>
 * To use this class, first write a class with template methods, e.g.
 * <code>src/main/java/mypackage/MyTemplates.java</code>. Then, also at
 * development time, execute {@link #main(String[])} with the class name of the
 * template class in the argument list, in this case
 * <code>{"mypackage.MyTemplates"}</code>. You can also specify a list of
 * classes containing templates.<br/>
 * As a result, a new java file will be generated for each processed class with
 * the postfix "Accessible" added to every processed class' name, in this case
 * <code>src/main/java/mypackage/MyTemplatesAccessible.java</code>.<br/>
 * You can now invoke <code>MyTemplatesAccessible.getMethods()</code> to
 * generate a map from method names to their sources.
 * </p>
 */
public class JavaTemplater {

	private static final String CLASS_START_PUBLIC = "\npublic class ";
	private static final String CLASS_START_NONPUBLIC = "\nclass ";
	final static String TEMPLATE_METHOD = "@" + TemplateMethod.class.getSimpleName();

	public static void main(String[] args) {
		if (args.length == 0) {
			String className = TemplateExample.class.getCanonicalName();
			System.out.println("no class name specified. processing default class " + className + ".");
			args = new String[] { className };
		}

		for (String className : args) {
			try {
				generateAccessibleSource(className);
			} catch (Exception e) {
				System.err.println("couldn't generate accessible source for class " + className);
				e.printStackTrace();
			}
		}
	}

	public static void generateAccessibleSource(String className) throws ClassNotFoundException, IOException {
		Compilation compi = new Compilation();

		compi.src.setClassName(className);
		compi.src.fileName = JavaSource.getJavaFilename(className);

		String dstClassName = className + "Accessible";
		compi.dst.setClassName(dstClassName);
		compi.dst.fileName = JavaSource.getJavaFilename(dstClassName);

		compi.dst.importList.add(Arrays.class.getCanonicalName());
		compi.dst.importList.add(HashMap.class.getCanonicalName());
		compi.dst.importList.add(null);
		compi.dst.importList.add(MethodSourceTemplate.class.getCanonicalName());
		compi.dst.importList.add(Param.class.getCanonicalName());

		compi.outBuf = new StringBuilder();
		compi.out = new Formatter(compi.outBuf);

		compi.src.body = loadFile(compi.src.fileName);

		parseSource(compi);

		compileJavaFile(compi);

		saveFile(compi.dst.fileName, compi.outBuf.toString());
	}

	private static void compileJavaFile(Compilation compi) {
		MethodSourceTemplate sourceOfGetMethodSourceMap = new MethodSourceTemplate();
		sourceOfGetMethodSourceMap.preamble = String.format("" + //
				"\n" + //
				"	/**\n" + //
				"	 * This method returns a map from method names to method sources.<br/>\n" + //
				"	 * The map is generated automatically based on the source of the class<br/>\n" + //
				"	 * %s.\n" + //
				"	 */\n" //
				, compi.src.simpleClassName);
		sourceOfGetMethodSourceMap.caption = "" + //
				"	public static HashMap<String, MethodSourceTemplate> getMethods() {\n" + //
				"		HashMap<String, MethodSourceTemplate> result = new HashMap<>();\n";
		sourceOfGetMethodSourceMap.body = "" + //
				"		result.put( //\n" + //
				"				\"%s\", //\n" + //
				"				new MethodSourceTemplate( //\n" + //
				"						\"%s\", //\n" + //
				"						\"%s\", //\n" + //
				"						\"%s\", //\n" + //
				"						\"%s\", //\n" + //
				"						%s, //\n" + //
				"						Arrays.<Param>asList( //\n" + //
				"%s" + //
				"						) //\n" + //
				"				) //\n" + //
				"		);\n";
		sourceOfGetMethodSourceMap.footer = "" + //
				"\n" + //
				"		return result;\n" + //
				"	}\n";

		generateJavaFileLeadingCode(compi);

		Formatter out = compi.out;

		out.format("\n");
		out.format(sourceOfGetMethodSourceMap.preamble);
		out.format(sourceOfGetMethodSourceMap.caption);

		for (MethodSourceTemplate methodSource : compi.methodSourceList) {
			String preamble = javaEscape(methodSource.preamble);
			String caption = javaEscape(methodSource.caption);
			String body = javaEscape(methodSource.body);
			String paramListStr = generateParamListCode(methodSource);

			out.format(sourceOfGetMethodSourceMap.body, //
					methodSource.name, //
					methodSource.name, //
					preamble, //
					caption, //
					body, //
					methodSource.mustInline, //
					paramListStr //
			);
		}

		out.format(sourceOfGetMethodSourceMap.footer);

		out.format("}\n");

		out.close();

	}

	private static void generateJavaFileLeadingCode(Compilation compi) {
		compi.out.flush();
		StringBuilder buf = compi.outBuf;

		String packageName = compi.dst.packageName;
		String preamble = compi.dst.preamble;
		String caption = compi.dst.caption;
		List<String> importList = compi.dst.importList;

		Pattern packageFinder = Pattern.compile("(?m)^package .*?;\n");
		Matcher packageMatcher = packageFinder.matcher(preamble);
		if (packageMatcher.find()) {
			int packageInPreambleEndIdx = packageMatcher.end();

			String preambleUpToPackageEnd = preamble.substring(0, packageInPreambleEndIdx);
			String preambleAfterPackageEnd = preamble.substring(packageInPreambleEndIdx);

			buf.append(preambleUpToPackageEnd);

			if (importList != null) {
				ifNotEmptyAppendLinefeed(buf);
				for (String imported : importList) {
					if (imported != null) {
						buf.append("import ").append(imported).append(";\n");
					} else {
						buf.append("\n");
					}
				}
				ifNotEmptyAppendLinefeed(buf);
			}

			preambleAfterPackageEnd = preambleAfterPackageEnd.trim();
			if (!preambleAfterPackageEnd.equals("")) {
				buf.append(preambleAfterPackageEnd).append("\n");
			}
		} else {
			if (packageName != null) {
				buf.append("package ").append(packageName).append(";\n");
			}

			if (importList != null) {
				ifNotEmptyAppendLinefeed(buf);
				for (String imported : importList) {
					buf.append("import ").append(imported).append(";\n");
				}
				ifNotEmptyAppendLinefeed(buf);
			}

			if (preamble != null) {
				buf.append(preamble);
			}
		}

		if (caption != null) {
			buf.append(caption);
		}
	}

	protected static void ifNotEmptyAppendLinefeed(StringBuilder buf) {
		if (buf.length() > 0) {
			buf.append("\n");
		}
	}

	protected static String generateParamListCode(MethodSourceTemplate methodSource) {
		StringBuilder paramListBuf = new StringBuilder();
		Formatter paramListFormatter = new Formatter(paramListBuf);
		for (Param param : methodSource.paramList) {
			paramListFormatter.format("								new Param(\"%s\", \"%s\"), //\n", //
					param.name, param.type);
		}

		paramListFormatter.close();
		int len = paramListBuf.length();
		if (paramListBuf.length() > 0) {
			paramListBuf.deleteCharAt(len - 5);
		}

		String paramListStr = paramListBuf.toString();
		return paramListStr;
	}

	private static void saveFile(String fileName, String content) throws IOException {
		FileWriter writer = new FileWriter(fileName);

		writer.write(content);

		writer.close();
	}

	private static String javaEscape(String s) {
		StringBuilder buf = new StringBuilder(s.length() + 10);

		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);

			switch (c) {
			case '\n':
				if (i < s.length() - 1) {
					buf.append("\\n\" + //\n\t\t\t\t\t\t\"");
				} else {
					buf.append("\\n");
				}
				break;
			case '"':
				buf.append("\\\"");
				break;
			case '\\':
				buf.append("\\\\");
				break;
			default:
				buf.append(c);
				break;
			}
		}

		return buf.toString();
	}

	private static void parseSource(Compilation compi) {
		ArrayList<MethodSourceTemplate> result = new ArrayList<>();

		String source = compi.src.body;

		int preambleStartIdx = 0;
		int preambleEndIdx = source.indexOf(CLASS_START_PUBLIC);
		if (preambleEndIdx == -1) {
			preambleEndIdx = source.indexOf(CLASS_START_NONPUBLIC);
			if (preambleEndIdx == -1) {
				throw new IllegalArgumentException(String.format("" + //
						"source file contain neither \"%s\" nor \"%s\"", //
						CLASS_START_PUBLIC.replace("\n", "\\n"), //
						CLASS_START_NONPUBLIC.replace("\n", "\\n") //
				));
			}
		}
		preambleEndIdx++;

		compi.dst.preamble = source.substring(preambleStartIdx, preambleEndIdx);

		compi.dst.preamble = compi.dst.preamble.replaceAll("(?m)^import .+?;.*?\n", "");
		compi.dst.preamble = compi.dst.preamble.replaceAll("\n{3,}", "\n\n");

		int captionStartIdx = preambleEndIdx;
		int captionEndIdx = source.indexOf('{', preambleEndIdx) + 1;

		compi.dst.caption = source.substring(captionStartIdx, captionEndIdx);
		compi.dst.caption = compi.dst.caption. //
				replace(compi.src.simpleClassName, compi.dst.simpleClassName);

		int idx = captionEndIdx;
		while (true) {
			idx = source.indexOf(TEMPLATE_METHOD, idx);
			if (idx == -1) {
				break;
			}

			MethodSourceTemplate methodSource = new MethodSourceTemplate();

			preambleStartIdx = source.lastIndexOf("\n\n", idx) + 1;
			preambleEndIdx = source.lastIndexOf('\n', idx) + 1;
			methodSource.preamble = source.substring(preambleStartIdx, preambleEndIdx);

			captionStartIdx = source.indexOf('\n', idx) + 1;
			captionEndIdx = source.indexOf('{', idx) + 2;
			methodSource.caption = source.substring(captionStartIdx, captionEndIdx);

			int methodNameEndIdx = methodSource.caption.indexOf('(');
			int methodNameStartIdx = methodSource.caption.lastIndexOf(' ', methodNameEndIdx) + 1;
			methodSource.name = methodSource.caption.substring(methodNameStartIdx, methodNameEndIdx);
			if (methodSource.name.endsWith("_mustInline")) {
				methodSource.name = methodSource.name.substring(0, methodSource.name.length() - "_mustInline".length());
				methodSource.mustInline = true;
			}

			int bodyStartIdx = captionEndIdx;
			int bodyEndIdx = getLastOfNestedCurlys(source, captionEndIdx - 2);
			bodyEndIdx = source.lastIndexOf('\n', bodyEndIdx) + 1;
			methodSource.body = source.substring(bodyStartIdx, bodyEndIdx);

			int paramListStartIdx = methodSource.caption.indexOf('(') + 1;
			int paramListEndIdx = methodSource.caption.indexOf(')', paramListStartIdx);
			String paramListStr = methodSource.caption.substring(paramListStartIdx, paramListEndIdx);

			ArrayList<Param> paramList = new ArrayList<>();

			if (!paramListStr.contains("<")) {
				String[] strParamList = paramListStr.split(",");
				for (String paramStr : strParamList) {
					paramStr = paramStr.trim();
					if (paramStr.isEmpty()) {
						continue;
					}

					int spaceIdx = paramStr.lastIndexOf(' ');
					Param param = new Param( //
							paramStr.substring(spaceIdx + 1), //
							paramStr.substring(0, spaceIdx) //
					);

					paramList.add(param);
				}
			}
			methodSource.paramList = paramList;

			result.add(methodSource);

			idx = bodyEndIdx;
		}

		compi.methodSourceList = result;
	}

	private static int getLastOfNestedCurlys(String source, int startIdx) {
		int curlyNestingDepth = 0;

		int idx = startIdx;
		for (; idx < source.length(); idx++) {
			char c = source.charAt(idx);

			if (c == '{') {
				curlyNestingDepth++;
			} else if (c == '}') {
				curlyNestingDepth--;

				if (curlyNestingDepth == -1) {
					throw new IllegalArgumentException(String.format("" + //
							"curly nesting is closed, but never opened, " + //
							"startIdx = %s; problem at %s", startIdx, idx));
				}

				if (curlyNestingDepth == 0) {
					break;
				}
			}
		}

		if (idx >= source.length()) {
			throw new IllegalArgumentException(String.format("" + //
					"curly nesting ended before it coult be finished, " + //
					"startIdx = %s; end at %s", startIdx, idx));
		}

		return idx;
	}

	private static String loadFile(String sourceFilename) throws IOException {
		File file = new File(sourceFilename);

		long fileLenL = file.length();
		if (fileLenL > Integer.MAX_VALUE) {
			throw new UnsupportedOperationException(String.format( //
					"files of length > %d are not supported. this file is of length %d.", //
					Integer.MAX_VALUE, fileLenL));
		}

		int fileLen = (int) fileLenL;

		byte[] buf = new byte[fileLen];
		FileInputStream fin = new FileInputStream(file);
		try {
			int totalReadLen = 0;
			while (totalReadLen < fileLen) {
				int readLen = fin.read(buf, totalReadLen, fileLen - totalReadLen);

				if (readLen == -1) {
					throw new IOException(String.format( //
							"could read only %d bytes of file of length %d and then the stream ended", //
							totalReadLen, fileLen));
				}

				totalReadLen += readLen;
			}
		} finally {
			fin.close();
		}

		String content = new String(buf);

		return content;
	}

	static class Compilation {
		JavaSource src = new JavaSource();

		JavaSource dst = new JavaSource();

		ArrayList<MethodSourceTemplate> methodSourceList;

		StringBuilder outBuf;

		Formatter out;
	}
}
