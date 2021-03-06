/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.runtime.state.filesystem;

import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;
import org.apache.flink.runtime.state.AbstractCloseableHandle;
import org.apache.flink.runtime.state.StateObject;
import org.apache.flink.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;

import static org.apache.flink.util.Preconditions.checkNotNull;

/**
 * Base class for state that is stored in a file.
 */
public abstract class AbstractFileStateHandle extends AbstractCloseableHandle implements StateObject {

	private static final long serialVersionUID = 350284443258002355L;

	private static final Logger LOG = LoggerFactory.getLogger(AbstractFileStateHandle.class);

	/** The path to the file in the filesystem, fully describing the file system */
	private final Path filePath;

	/** Cached file system handle */
	private transient FileSystem fs;

	/**
	 * Creates a new file state for the given file path.
	 * 
	 * @param filePath The path to the file that stores the state.
	 */
	protected AbstractFileStateHandle(Path filePath) {
		this.filePath = checkNotNull(filePath);
	}

	/**
	 * Gets the path where this handle's state is stored.
	 * @return The path where this handle's state is stored.
	 */
	public Path getFilePath() {
		return filePath;
	}

	/**
	 * Discard the state by deleting the file that stores the state. If the parent directory
	 * of the state is empty after deleting the state file, it is also deleted.
	 * 
	 * @throws Exception Thrown, if the file deletion (not the directory deletion) fails.
	 */
	@Override
	public void discardState() throws Exception {
		getFileSystem().delete(filePath, false);

		try {
			FileUtils.deletePathIfEmpty(getFileSystem(), filePath.getParent());
		} catch (Exception ignored) {}
	}

	/**
	 * Gets the file system that stores the file state.
	 * @return The file system that stores the file state.
	 * @throws IOException Thrown if the file system cannot be accessed.
	 */
	protected FileSystem getFileSystem() throws IOException {
		if (fs == null) {
			fs = FileSystem.get(filePath.toUri());
		}
		return fs;
	}

	/**
	 * Returns the file size in bytes.
	 *
	 * @return The file size in bytes.
	 * @throws IOException Thrown if the file system cannot be accessed.
	 */
	protected long getFileSize() throws IOException {
		try {
			return getFileSystem().getFileStatus(filePath).getLen();
		}
		catch (FileNotFoundException e) {
			LOG.info("Could not determine state size - state will appear as size '0'. " +
					"If the underlying filesystem is eventually consistency, that is most likely the cause.");
		}
		catch (IOException e) {
			// this can happen on eventually consistent file systems (like for example S3)
			LOG.info("Could not determine state size - state will appear as size '0'.", e);
		}

		return 0;
	}
}
