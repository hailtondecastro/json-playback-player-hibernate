package org.jsplayback.backend;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;

public interface IDirectRawWriter {
	void write(OutputStream outputStream) throws IOException, SQLException;
}
