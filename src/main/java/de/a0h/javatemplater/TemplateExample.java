package de.a0h.javatemplater;

public class TemplateExample {

	/**
	 * This is an example method for a source template which is processed by the
	 * JavaTemplater in this package.
	 */
	@TemplateMethod
	public static final void sigmoid(float[] inp, float[] out, int len) {
		for (int i = 0; i < len; i++) {
			out[i] = 1.0f / (1.0f + (float) Math.exp(-inp[i]));
		}
	}
}
