package spoon.test.prettyprinter;

import org.apache.commons.io.*;
import org.hamcrest.*;
import org.junit.*;
import static org.junit.Assert.*;

import spoon.Launcher;
import spoon.compiler.*;
import spoon.reflect.*;
import spoon.reflect.code.CtComment.*;
import spoon.reflect.declaration.*;
import spoon.reflect.visitor.filter.*;
import spoon.support.sniper.*;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;

public class SniperAssertTest {
	private static final Path INPUT_PATH = Paths.get("src/test/java/");
	private static final Path OUTPUT_PATH = Paths.get("target/test-output");

	@BeforeClass
	public static void setup() throws IOException {
		FileUtils.deleteDirectory(OUTPUT_PATH.toFile());
	}

	@Test
	public void assertNOK() throws IOException {
		runSniperJavaPrettyPrinter("spoon/test/prettyprinter/testclasses/asserttest/AssertNOK.java");
	}

	@Test
	public void assertOk() throws IOException {
		runSniperJavaPrettyPrinter("spoon/test/prettyprinter/testclasses/asserttest/AssertOk.java");
	}

	private void runSniperJavaPrettyPrinter(String path) throws IOException {
		final Launcher launcher = new Launcher();
		final Environment e = launcher.getEnvironment();
		e.setLevel("INFO");
		e.setPrettyPrinterCreator(() -> new SniperJavaPrettyPrinter(e));

		launcher.addInputResource(INPUT_PATH.resolve(path).toString());
		launcher.setSourceOutputDirectory(OUTPUT_PATH.toString());

		CtModel model = launcher.buildModel();
		CtMethod method = model.getElements(new TypeFilter<>(CtMethod.class)).get(0);

		method.getBody().insertBegin(launcher.getFactory().Code().createComment("test", CommentType.BLOCK));

		launcher.process();
		launcher.prettyprint();
		// Verify result file exist and is not empty
		assertThat("Output file for " + path + " should exist", OUTPUT_PATH.resolve(path).toFile().exists(),
				CoreMatchers.equalTo(true));

		String content = new String(Files.readAllBytes(OUTPUT_PATH.resolve(path)), StandardCharsets.UTF_8);

		assertThat(content, CoreMatchers.notNullValue());
		assertThat("Result class should not be empty", content.trim(), CoreMatchers.not(CoreMatchers.equalTo("")));
		assertThat("Should contain assert with semicolon", content.trim(), CoreMatchers.containsString("assert true;"));
	}
}
