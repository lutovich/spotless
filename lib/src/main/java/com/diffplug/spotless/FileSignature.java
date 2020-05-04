/*
 * Copyright 2016 DiffPlug
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.diffplug.spotless;

import static com.diffplug.spotless.MoreIterables.toNullHostileList;
import static com.diffplug.spotless.MoreIterables.toSortedSet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/** Computes a signature for any needed files. */
public final class FileSignature implements Serializable {
	private static final long serialVersionUID = 1L;

	/*
	 * Transient because not needed to uniquely identify a FileSignature instance, and also because
	 * Gradle only needs this class to be Serializable so it can compare FileSignature instances for
	 * incremental builds.
	 */
	@SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
	private final transient List<File> files;

	private final byte[] digest;

	/** Method has been renamed to {@link FileSignature#signAsSet}.
	 * In case no sorting and removal of duplicates is required,
	 * use {@link FileSignature#signAsList} instead.*/
	@Deprecated
	public static FileSignature from(File... files) throws IOException {
		return from(Arrays.asList(files));
	}

	/** Method has been renamed to {@link FileSignature#signAsSet}.
	 * In case no sorting and removal of duplicates is required,
	 * use {@link FileSignature#signAsList} instead.*/
	@Deprecated
	public static FileSignature from(Iterable<File> files) throws IOException {
		return signAsSet(files);
	}

	/** Creates file signature whereas order of the files remains unchanged. */
	public static FileSignature signAsList(File... files) throws IOException {
		return signAsList(Arrays.asList(files));
	}

	/** Creates file signature whereas order of the files remains unchanged. */
	public static FileSignature signAsList(Iterable<File> files) throws IOException {
		return new FileSignature(toNullHostileList(files));
	}

	/** Creates file signature whereas order of the files remains unchanged. */
	public static FileSignature signAsSet(File... files) throws IOException {
		return signAsSet(Arrays.asList(files));
	}

	/** Creates file signature insensitive to the order of the files. */
	public static FileSignature signAsSet(Iterable<File> files) throws IOException {
		return new FileSignature(toSortedSet(files));
	}

	private FileSignature(final List<File> files) throws IOException {
		this.files = files;
		this.digest = digestOf(files);
	}

	/** Returns all of the files in this signature, throwing an exception if there are more or less than 1 file. */
	public Collection<File> files() {
		return Collections.unmodifiableList(files);
	}

	/** Returns the only file in this signature, throwing an exception if there are more or less than 1 file. */
	public File getOnlyFile() {
		if (files.size() == 1) {
			return files.iterator().next();
		} else {
			throw new IllegalArgumentException("Expected one file, but was " + files.size());
		}
	}

	private static byte[] digestOf(List<File> files) throws IOException {
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
			for(File file : files) {
				byte[] fileBytes = Files.readAllBytes(file.toPath());
				messageDigest.update(fileBytes);
			}
			return messageDigest.digest();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}
}
