package de.a0h.javatemplater;

import java.util.Arrays;
import java.util.HashMap;

import de.a0h.javatemplater.MethodSourceTemplate;
import de.a0h.javatemplater.MethodSourceTemplate.Param;

public class TemplateExampleAccessible {

	/**
	 * This method returns a map from method names to method sources.<br/>
	 * The map is generated automatically based on the source of the class<br/>
	 * TemplateExample.
	 */
	public static HashMap<String, MethodSourceTemplate> getMethods() {
		HashMap<String, MethodSourceTemplate> result = new HashMap<>();
		result.put( //
				"sigmoid", //
				new MethodSourceTemplate( //
						"sigmoid", //
						"\n" + //
						"	/**\n" + //
						"	 * This is an example method for a source template which is processed by the\n" + //
						"	 * JavaTemplater in this package.\n" + //
						"	 */\n", //
						"	public static final void sigmoid(float[] inp, float[] out, int len) {\n", //
						"		for (int i = 0; i < len; i++) {\n" + //
						"			out[i] = 1.0f / (1.0f + (float) Math.exp(-inp[i]));\n" + //
						"		}\n", //
						false, //
						Arrays.<Param>asList( //
								new Param("inp", "float[]"), //
								new Param("out", "float[]"), //
								new Param("len", "int") //
						) //
				) //
		);

		return result;
	}
}
