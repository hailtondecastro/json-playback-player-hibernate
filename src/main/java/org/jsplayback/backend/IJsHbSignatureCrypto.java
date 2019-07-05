package org.jsplayback.backend;

/**
 * Encript and decript from and to Json signature.
 * @author Hailton de Castro
 *
 */
public interface IJsHbSignatureCrypto {
	/**
	 * Returns a 'Base64 url safe String' from original Json signature.  
	 * @param plainSignatureStr
	 * @return
	 */
	String encrypt(String plainSignatureStr);
	/**
	 * Returns the original Json signature from encrypted 'Base64 url safe String'.
	 * @param disgestedSignatureStr
	 * @return
	 */
	String decrypt(String disgestedSignatureStr);
}
/*gerando conflito*/