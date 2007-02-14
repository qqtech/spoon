/* 
 * Spoon - http://spoon.gforge.inria.fr/
 * Copyright (C) 2006 INRIA Futurs <renaud.pawlak@inria.fr>
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

package spoon.support;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;

import spoon.processing.AbstractProcessor;
import spoon.processing.FileGenerator;
import spoon.reflect.declaration.CtSimpleType;
import spoon.support.util.JDTCompiler;

/**
 * This class defines a processor that generates class files from the metamodel.
 */
public class ByteCodeOutputProcessor extends AbstractProcessor<CtSimpleType<?>>
		implements FileGenerator<CtSimpleType<?>> {

	/**
	 * Extension for class files (.class).
	 */
	public static final String CLASS_EXT = ".class";

	private File outputDir;

	private List<ICompilationUnit> units = new ArrayList<ICompilationUnit>();

	private List<File> printed = new ArrayList<File>();

	private JavaOutputProcessor javaPrinter;

	/**
	 * Creates a new processor for generating Java source files.
	 * 
	 * @param outputDirectory
	 *            the root output directory
	 */
	public ByteCodeOutputProcessor(JavaOutputProcessor javaPrinter,
			File outputDirectory) {
		this.outputDir = outputDirectory;
		this.javaPrinter = javaPrinter;
	}

	public List<File> getCreatedFiles() {
		return printed;
	}

	public File getOutputDirectory() {
		return outputDir;
	}

	/**
	 * Tells if the source is Java 1.4 or lower.
	 */
	public long getJavaCompliance() {
		switch (getFactory().getEnvironment().getComplianceLevel()) {
		case 1:
			return ClassFileConstants.JDK1_1;
		case 2:
			return ClassFileConstants.JDK1_2;
		case 3:
			return ClassFileConstants.JDK1_3;
		case 4:
			return ClassFileConstants.JDK1_4;
		case 5:
			return ClassFileConstants.JDK1_5;
		case 6:
			return ClassFileConstants.JDK1_6;
		}
		return ClassFileConstants.JDK1_5;
	}

	public void process(CtSimpleType<?> element) {
		if (!element.isTopLevel())
			return;
		// Create Java code and create ICompilationUnit
		javaPrinter.getCreatedFiles().clear();
		javaPrinter.createJavaFile(element);

		for (File f : javaPrinter.getCreatedFiles()) {
			try {
				units.add(JDTCompiler.getUnit(element.getQualifiedName(), f));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	@Override
	public void processingDone() {
		try {
			// Do compilation
			JDTCompiler compiler = new JDTCompiler();
			compiler.getCompilerOption().sourceLevel = getJavaCompliance();
			compiler.compile(units.toArray(new ICompilationUnit[0]));

			getOutputDirectory().mkdirs();

			for (ClassFile f : compiler.getClassFiles()) {

				ClassFile.writeToDisk(true, getOutputDirectory()
						.getAbsolutePath(), new String(f.fileName()).replace(
						'/', File.separatorChar)
						+ CLASS_EXT, f);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void setOutputDirectory(File directory) {
		this.outputDir = directory;
	}

}