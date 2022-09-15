package de.a0h.javatemplater;

import java.util.ArrayList;
import java.util.List;

public class MethodSourceTemplate {

	public static class Param {

		public String name;
		public String type;

		public Param() {
		}

		public Param(String name, String type) {
			this.name = name;
			this.type = type;
		}
	}

	public String name;

	public boolean mustInline;

	public String preamble;

	public String caption;

	public String body;

	public String footer = "\t}\n";

	/**
	 * List of String pairs as 2-element arrays, with [0] containing the parameter
	 * type and [1] holding its name.
	 */
	public List<Param> paramList;

	public MethodSourceTemplate() {
		paramList = new ArrayList<>();
	}

	public MethodSourceTemplate(String name, String preamble, String caption, String body, boolean mustInline) {
		this(name, preamble, caption, body, mustInline, null);
	}

	public MethodSourceTemplate(String name, String preamble, String caption, String body, boolean mustInline,
			List<Param> paramList) {
		this.name = name;
		this.preamble = preamble;
		this.caption = caption;
		this.body = body;
		this.mustInline = mustInline;
		this.paramList = paramList;
	}

	public void generateSource(StringBuilder buf) {
		buf.append(preamble);
		buf.append(caption);
		buf.append(body);
		buf.append(footer);
	}
}
