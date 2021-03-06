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
package com.diffplug.spotless.generic;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;

import org.junit.Assert;
import org.junit.Test;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.SerializableEqualityTester;
import com.diffplug.spotless.StepHarness;

public class LicenseHeaderStepTest extends ResourceHarness {
	private static final String KEY_LICENSE = "license/TestLicense";
	private static final String KEY_FILE_NOTAPPLIED = "license/MissingLicense.test";
	private static final String KEY_FILE_APPLIED = "license/HasLicense.test";

	// files to test $YEAR token replacement
	private static final String KEY_LICENSE_WITH_YEAR_TOKEN = "license/LicenseHeaderWithYearToken";
	private static final String KEY_FILE_WITHOUT_LICENSE = "license/FileWithoutLicenseHeader.test";
	private static final String KEY_FILE_WITH_LICENSE_AND_PLACEHOLDER = "license/FileWithLicenseHeaderAndPlaceholder.test";

	// If this constant changes, don't forget to change the similarly-named one in
	// plugin-gradle/src/main/java/com/diffplug/gradle/spotless/JavaExtension.java as well
	private static final String LICENSE_HEADER_DELIMITER = "package ";

	@Test
	public void fromHeader() throws Throwable {
		FormatterStep step = LicenseHeaderStep.createFromHeader(getTestResource(KEY_LICENSE), LICENSE_HEADER_DELIMITER);
		assertOnResources(step, KEY_FILE_NOTAPPLIED, KEY_FILE_APPLIED);
	}

	@Test
	public void fromFile() throws Throwable {
		FormatterStep step = LicenseHeaderStep.createFromFile(createTestFile(KEY_LICENSE), StandardCharsets.UTF_8, LICENSE_HEADER_DELIMITER);
		assertOnResources(step, KEY_FILE_NOTAPPLIED, KEY_FILE_APPLIED);
	}

	@Test
	public void should_apply_license_containing_YEAR_token() throws Throwable {
		FormatterStep step = LicenseHeaderStep.createFromFile(createTestFile(KEY_LICENSE_WITH_YEAR_TOKEN), StandardCharsets.UTF_8, LICENSE_HEADER_DELIMITER);

		StepHarness.forStep(step)
				.test(getTestResource(KEY_FILE_WITHOUT_LICENSE), fileWithPlaceholderContaining(currentYear()))
				.testUnaffected(fileWithPlaceholderContaining(currentYear()))
				.testUnaffected(fileWithPlaceholderContaining("2003"))
				.testUnaffected(fileWithPlaceholderContaining("1990-2015"))
				.test(fileWithPlaceholderContaining("not a year"), fileWithPlaceholderContaining(currentYear()));
	}

	private String fileWithPlaceholderContaining(String placeHolderContent) throws IOException {
		return getTestResource(KEY_FILE_WITH_LICENSE_AND_PLACEHOLDER).replace("__PLACEHOLDER__", placeHolderContent);
	}

	private String currentYear() {
		return String.valueOf(YearMonth.now().getYear());
	}

	@Test
	public void efficient() throws Throwable {
		FormatterStep step = LicenseHeaderStep.createFromHeader("LicenseHeader\n", "contentstart");
		String alreadyCorrect = "LicenseHeader\ncontentstart";
		Assert.assertEquals(alreadyCorrect, step.format(alreadyCorrect, new File("")));
		// If no change is required, it should return the exact same string for efficiency reasons
		Assert.assertSame(alreadyCorrect, step.format(alreadyCorrect, new File("")));
	}

	@Test
	public void sanitized() throws Throwable {
		// The sanitizer should add a \n
		FormatterStep step = LicenseHeaderStep.createFromHeader("LicenseHeader", "contentstart");
		String alreadyCorrect = "LicenseHeader\ncontentstart";
		Assert.assertEquals(alreadyCorrect, step.format(alreadyCorrect, new File("")));
		Assert.assertSame(alreadyCorrect, step.format(alreadyCorrect, new File("")));
	}

	@Test
	public void sanitizerDoesntGoTooFar() throws Throwable {
		// if the user wants extra lines after the header, we shouldn't clobber them
		FormatterStep step = LicenseHeaderStep.createFromHeader("LicenseHeader\n\n", "contentstart");
		String alreadyCorrect = "LicenseHeader\n\ncontentstart";
		Assert.assertEquals(alreadyCorrect, step.format(alreadyCorrect, new File("")));
		Assert.assertSame(alreadyCorrect, step.format(alreadyCorrect, new File("")));
	}

	@Test
	public void equality() {
		new SerializableEqualityTester() {
			String header = "LICENSE";
			String delimiter = "package";

			@Override
			protected void setupTest(API api) {
				api.areDifferentThan();

				delimiter = "crate";
				api.areDifferentThan();

				header = "APACHE";
				api.areDifferentThan();

				delimiter = "package";
				api.areDifferentThan();
			}

			@Override
			protected FormatterStep create() {
				return LicenseHeaderStep.createFromHeader(header, delimiter);
			}
		}.testEquals();
	}
}
