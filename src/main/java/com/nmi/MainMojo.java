package com.nmi;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.MessageFormat;

@Mojo(name = "dto2ts")
public class MainMojo extends AbstractMojo {
    /**
     * A modifier indicating a public field
     */
    private final int modifierPublic = 1;

    @Parameter
    private String[] classes;

    @Parameter
    private String suffix;

    @Parameter
    private String prefix;

    @Parameter
    private String outputFolder;

    @Parameter
    private boolean overwrite = true;

    @Parameter
    private boolean addNamespace = false;

    @Parameter
    private boolean includePrivate = true;

    @Parameter
    private String namespace;

    private StringBuffer addType(StringBuffer str, Field field) {
        String retVal = "any";
        String type = field.getType().getSimpleName().toString();
        String typeComment = "";

        if (type.startsWith("String")) {
            retVal = "String";
        } else if (type.startsWith("Date")) {
            retVal = "String";
            typeComment = "// " + field.getType();
        } else if (type.toLowerCase().startsWith("boolean")) {
            retVal = "boolean";
        } else if (type.toLowerCase().startsWith("byte") ||
                type.toLowerCase().startsWith("short") ||
                type.toLowerCase().startsWith("int") ||
                type.toLowerCase().startsWith("long") ||
                type.toLowerCase().startsWith("float") ||
                type.toLowerCase().startsWith("double")) {
            retVal = "number";
            typeComment = "// " + type;
        }

        if (type.endsWith("[]")) {
            retVal = "Array<" + retVal + ">";
        }

        return str.append(retVal + "; " + typeComment + "\n");
    }

    public void execute() throws MojoExecutionException {
        if (StringUtils.isNotBlank(outputFolder) && !outputFolder.endsWith("/")) {
            outputFolder += "/";
        }

        getLog().info("Processing classes to TypeScript.");
        for (String clazz : classes) {
            try {
                final Class<?> aClass = Class.forName(clazz);
                String outputTS =
                        StringUtils.defaultIfBlank(outputFolder, "") +
                                StringUtils.defaultIfBlank(prefix, "") +
                                StringUtils.uncapitalize(aClass.getSimpleName()) +
                                StringUtils.defaultIfBlank(suffix, "") +
                                ".ts";
                getLog().info(MessageFormat.format(
                        "   * Converting class {0} to {1}.", clazz, outputTS));
                if (new File(outputTS).isFile() && !overwrite) {
                    getLog().info("        Ignored, already exists.");
                    continue;
                }

                StringBuffer outputStr = new StringBuffer();
                if (addNamespace) {
                    outputStr.append("namespace ");
                    if (StringUtils.isNotBlank(namespace)) {
                        outputStr.append(namespace);
                    } else {
                        outputStr.append(aClass.getPackage().getName());
                    }
                    outputStr.append(" {\n");
                }
                outputStr.append("export class ");
                outputStr.append(aClass.getSimpleName());
                outputStr.append(" {\n");
                final Field[] allFields = FieldUtils.getAllFields(aClass);
                for (Field field : allFields) {
                    if (field.getModifiers() != modifierPublic && !includePrivate) {
                        continue;
                    }
                    outputStr.append("\tpublic ");
                    outputStr.append(field.getName());
                    outputStr.append(": ");
                    outputStr = addType(outputStr, field);
                }
                outputStr.append("}\n");
                if (addNamespace) {
                    outputStr.append("}\n");
                }
                FileUtils.writeStringToFile(new File(outputTS),
                        outputStr.toString(), "UTF-8");
            } catch (ClassNotFoundException | IOException e) {
                getLog().error(e);
            }
        }
    }
}
