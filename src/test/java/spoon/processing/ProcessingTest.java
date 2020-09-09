/**
 * Copyright (C) 2006-2018 INRIA and contributors
 * Spoon - http://spoon.gforge.inria.fr/
 *
 * This software is governed by the CeCILL-C License under French law and
 * abiding by the rules of distribution of free software. You can use, modify
 * and/or redistribute the software under the terms of the CeCILL-C license as
 * circulated by CEA, CNRS and INRIA at http://www.cecill.info.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the CeCILL-C License for more details.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-C license and that you accept its terms.
 */
package spoon.processing;

import org.junit.Test;
import spoon.Launcher;
import spoon.compiler.Environment;
import spoon.reflect.code.CtAssert;
import spoon.support.compiler.FileSystemFile;
import spoon.support.sniper.SniperJavaPrettyPrinter;
import spoon.test.processing.processors.MyProcessor;
import spoon.test.template.testclasses.AssertToIfAssertedStatementTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static spoon.testing.Assert.assertThat;

public class ProcessingTest {

	@Test
	public void testInterruptAProcessor() {
		final Launcher launcher = new Launcher();
		launcher.getEnvironment().setNoClasspath(true);
		launcher.setArgs(new String[] {"--output-type", "nooutput" });
		launcher.addInputResource("./src/test/java/spoon/processing/");
		final MyProcessor processor = new MyProcessor();
		launcher.addProcessor(processor);
		try {
			launcher.run();
		} catch (ProcessInterruption e) {
			fail("ProcessInterrupt exception must be catch in the ProcessingManager.");
		}
		assertFalse(processor.isShouldStayAtFalse());
	}

	@Test
	public void testSpoonTagger() {
		final Launcher launcher = new Launcher();
		launcher.addProcessor("spoon.processing.SpoonTagger");
		launcher.run();
		assertTrue(new File(launcher.getModelBuilder().getSourceOutputDirectory() + "/spoon/Spoon.java").exists());
	}

	@Test
	public void testStaticImport() throws IOException {
		final Launcher l = new Launcher();
		Environment e = l.getEnvironment();

		String[] sourcePath = new String[0];
		e.setNoClasspath(false);
		e.setSourceClasspath(sourcePath);
		e.setAutoImports(true);
		e.setPrettyPrinterCreator(() -> new SniperJavaPrettyPrinter(l.getEnvironment()));

		Path path = Files.createTempDirectory("emptydir");
		l.addInputResource("src/test/resources/compilation3/A.java");
		l.addInputResource("src/test/resources/compilation3/subpackage/B.java");
		l.setSourceOutputDirectory(path.toFile());
		l.run();
	}

	@Test
	public void testNullPointerException() throws IOException {
		// https://github.com/INRIA/spoon/pull/3254
		final Launcher l = new Launcher();
		Environment e = l.getEnvironment();

		e.setNoClasspath(true);
		e.setAutoImports(true);
		e.setPrettyPrinterCreator(() -> new SniperJavaPrettyPrinter(l.getEnvironment()));

		Path path = Files.createTempDirectory("emptydir");
		l.addInputResource("src/test/resources/compilation5/A.java");
		l.setSourceOutputDirectory(path.toFile());
		l.run();
	}
	
	@Test
	public void testTemplateNotInOutput() throws IOException {
		// https://github.com/INRIA/spoon/issues/2987
		class AssertProcessor extends AbstractProcessor<CtAssert<?>> {
			public void process(CtAssert<?> element) {
				element.replace(new AssertToIfAssertedStatementTemplate(element).apply(null));
			}
		}
		
		String templatePath = "src/test/java/spoon/test/template/testclasses/AssertToIfAssertedStatementTemplate.java";
		String resourcePath = "src/test/resources/spoon/test/template/";
		
		final Launcher l = new Launcher();
		Path path = Files.createTempDirectory("emptydir");
		
		l.addProcessor(new AssertProcessor());
		l.addTemplateResource(new FileSystemFile(templatePath));
		
		l.addInputResource(resourcePath + "SimpleAssert.java");
		l.setSourceOutputDirectory(path.toFile());
		l.run();

		// If template is applied to itself then there will be modified spoon/...Template.java on output
		assertArrayEquals("Template source found in output", new String[]{"SimpleAssert.java"}, path.toFile().list());
		// Check that the template worked as intended
		assertThat(path.toString() + "/SimpleAssert.java")
			.isEqualTo(resourcePath + "SimpleIfAsserted.java");
	}
}
