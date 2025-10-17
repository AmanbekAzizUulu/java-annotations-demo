// src/main/java/com/dandaev/edu/annotation/processor/AdvancedAnnotationProcessor.java
package com.dandaev.edu.annotation.processor;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import com.dandaev.edu.annotations.builder.pattern.generator.DefaultValue;
import com.dandaev.edu.annotations.builder.pattern.generator.GenerateBuilder;
import com.dandaev.edu.annotations.builder.pattern.generator.GenerateToString;

@SupportedAnnotationTypes("com.dandaev.edu.annotations.builder.pattern.generator.*")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class AdvancedAnnotationProcessor extends AbstractProcessor {

	private Filer filer;
	private Messager messager;

	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);
		this.filer = processingEnv.getFiler();
		this.messager = processingEnv.getMessager();
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		messager.printMessage(Diagnostic.Kind.NOTE, ">>> Processor started!");
		System.out.println(">>> Processor started!");
		try {
			// Генерируем Builder для аннотированных классов
			for (Element element : roundEnv.getElementsAnnotatedWith(GenerateBuilder.class)) {
				if (element.getKind() == ElementKind.CLASS) {
					generateAdvancedBuilder((TypeElement) element);
				}
			}

			// Генерируем ToString для аннотированных классов
			for (Element element : roundEnv.getElementsAnnotatedWith(GenerateToString.class)) {
				if (element.getKind() == ElementKind.CLASS) {
					generateAdvancedToString((TypeElement) element);
				}
			}
		} catch (Exception e) {
			error("Processing failed: " + e.getMessage());
			e.printStackTrace();
		}
		return true;
	}

	private void generateAdvancedBuilder(TypeElement classElement) throws IOException {
		String packageName = getPackageName(classElement);
		String className = classElement.getSimpleName().toString();
		GenerateBuilder annotation = classElement.getAnnotation(GenerateBuilder.class);
		String builderName = annotation.builderName().isEmpty() ? className + "Builder" : annotation.builderName();

		// Собираем все поля класса
		List<VariableElement> fields = getAllFields(classElement);

		// Генерируем исходный код
		JavaFileObject file = filer.createSourceFile(packageName + "." + builderName);
		try (PrintWriter writer = new PrintWriter(file.openWriter())) {
			writer.println("package " + packageName + ";");
			writer.println();
			writer.println("import java.util.*;");
			writer.println();
			writer.println("/**");
			writer.println(" * Auto-generated Builder for " + className);
			writer.println(" */");
			writer.println("public final class " + builderName + " {");
			writer.println();

			// Поля builder'а
			for (VariableElement field : fields) {
				String fieldType = field.asType().toString();
				String fieldName = field.getSimpleName().toString();
				String defaultValue = getSmartDefaultValue(field);
				writer.println("    private " + fieldType + " " + fieldName + " = " + defaultValue + ";");
			}
			writer.println();

			// Fluent setter методы
			for (VariableElement field : fields) {
				String fieldType = field.asType().toString();
				String fieldName = field.getSimpleName().toString();
				String methodName = fieldName;

				writer.println(
						"    public " + builderName + " " + methodName + "(" + fieldType + " " + fieldName + ") {");
				writer.println("        this." + fieldName + " = " + fieldName + ";");
				writer.println("        return this;");
				writer.println("    }");
				writer.println();
			}

			// Метод build()
			writer.println("    public " + className + " build() {");
			writer.println("        " + className + " result = new " + className + "();");
			writer.println("        try {");

			for (VariableElement field : fields) {
				String fieldName = field.getSimpleName().toString();
				writer.println("            java.lang.reflect.Field " + fieldName + "Field = " +
						className + ".class.getDeclaredField(\"" + fieldName + "\");");
				writer.println("            " + fieldName + "Field.setAccessible(true);");
				writer.println("            " + fieldName + "Field.set(result, this." + fieldName + ");");
			}

			writer.println("        } catch (Exception e) {");
			writer.println("            throw new RuntimeException(\"Failed to build " + className + "\", e);");
			writer.println("        }");
			writer.println("        return result;");
			writer.println("    }");
			writer.println("}");
		}

		note("Generated builder: " + builderName);
	}

	private void generateAdvancedToString(TypeElement classElement) throws IOException {
		String packageName = getPackageName(classElement);
		String className = classElement.getSimpleName().toString();

		List<VariableElement> fields = getAllFields(classElement);

		JavaFileObject file = filer.createSourceFile(packageName + "." + className + "ToStringHelper");
		try (PrintWriter writer = new PrintWriter(file.openWriter())) {
			writer.println("package " + packageName + ";");
			writer.println();
			writer.println("/**");
			writer.println(" * ToString helper for " + className);
			writer.println(" */");
			writer.println("public class " + className + "ToStringHelper {");
			writer.println();
			writer.println("    public static String toString(" + className + " obj) {");
			writer.println("        if (obj == null) return \"null\";");
			writer.println("        ");
			writer.println("        StringBuilder sb = new StringBuilder();");
			writer.println("        sb.append(\"" + className + "{\");");
			writer.println("        ");
			writer.println("        try {");

			for (int i = 0; i < fields.size(); i++) {
				VariableElement field = fields.get(i);
				String fieldName = field.getSimpleName().toString();
				String comma = i < fields.size() - 1 ? "sb.append(\", \");" : "";

				writer.println("            java.lang.reflect.Field " + fieldName + "Field = " +
						className + ".class.getDeclaredField(\"" + fieldName + "\");");
				writer.println("            " + fieldName + "Field.setAccessible(true);");
				writer.println(
						"            sb.append(\"" + fieldName + "=\").append(" + fieldName + "Field.get(obj));");
				if (!comma.isEmpty()) {
					writer.println("            " + comma);
				}
			}

			writer.println("        } catch (Exception e) {");
			writer.println("            // Ignore reflection errors in toString");
			writer.println("        }");
			writer.println("        ");
			writer.println("        sb.append(\"}\");");
			writer.println("        return sb.toString();");
			writer.println("    }");
			writer.println("}");
		}

		note("Generated toString helper for: " + className);
	}

	private List<VariableElement> getAllFields(TypeElement classElement) {
		List<VariableElement> fields = new ArrayList<>();
		for (Element enclosed : classElement.getEnclosedElements()) {
			if (enclosed.getKind() == ElementKind.FIELD) {
				fields.add((VariableElement) enclosed);
			}
		}
		return fields;
	}

	private String getPackageName(TypeElement classElement) {
		return processingEnv.getElementUtils()
				.getPackageOf(classElement)
				.getQualifiedName()
				.toString();
	}

	private String getSmartDefaultValue(VariableElement field) {
		DefaultValue defaultValueAnn = field.getAnnotation(DefaultValue.class);
		if (defaultValueAnn != null) {
			String value = defaultValueAnn.value();
			TypeKind kind = field.asType().getKind();

			switch (kind) {
				case INT:
					return value;
				case LONG:
					return value + "L";
				case DOUBLE:
					return value;
				case FLOAT:
					return value + "f";
				case BOOLEAN:
					return value;
				default:
					return "\"" + value + "\"";
			}
		}

		// Умные значения по умолчанию на основе типа
		TypeMirror type = field.asType();
		switch (type.getKind()) {
			case INT:
				return "0";
			case LONG:
				return "0L";
			case DOUBLE:
				return "0.0";
			case FLOAT:
				return "0.0f";
			case BOOLEAN:
				return "false";
			case DECLARED:
				String typeName = type.toString();
				if (typeName.equals("java.lang.String"))
					return "null";
				if (typeName.startsWith("java.util.List"))
					return "new java.util.ArrayList<>()";
				if (typeName.startsWith("java.util.Set"))
					return "new java.util.HashSet<>()";
				if (typeName.startsWith("java.util.Map"))
					return "new java.util.HashMap<>()";
				return "null";
			default:
				return "null";
		}
	}

	private void note(String msg) {
		processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, msg);
	}

	private void error(String msg) {
		processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg);
	}
}
