package ir.ebb.common.repository;

import com.palantir.javapoet.*;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import io.r2dbc.spi.Statement;
import org.apache.pekko.Done;
import org.apache.pekko.projection.r2dbc.javadsl.R2dbcSession;

/**
 * Annotation processor that generates repository implementations for entities
 * annotated with @GenerateRepository.
 */
@SupportedAnnotationTypes("ir.ebb.common.repository.GenerateRepository")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class RepositoryProcessor extends AbstractProcessor {

    private Elements elementUtils;
    private Types typeUtils;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        elementUtils = processingEnv.getElementUtils();
        typeUtils = processingEnv.getTypeUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }

        for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(GenerateRepository.class)) {
            if (annotatedElement.getKind() != ElementKind.RECORD && annotatedElement.getKind() != ElementKind.CLASS) {
                processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "@GenerateRepository can only be applied to record or regular classes",
                    annotatedElement
                );
                continue;
            }

            try {
                generateRepository((TypeElement) annotatedElement);
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Failed to generate repository for " + annotatedElement.getSimpleName() + ": " + e.getMessage(),
                    annotatedElement
                );
            }
        }

        return true;
    }

    private void generateRepository(TypeElement entityElement) throws IOException {
        GenerateRepository annotation = entityElement.getAnnotation(GenerateRepository.class);

        String entityName = entityElement.getSimpleName().toString();
        String repositoryName = getRepositoryName(entityName, annotation);
        String packageName = getPackageName(entityElement, annotation);
        String tableName = getTableName(entityName, annotation);

        List<FieldElement> fields = getFields(entityElement);

        // Find primary key info
        FieldInfo primaryKey = findPrimaryKey(fields);

        TypeSpec repositoryClass = TypeSpec.classBuilder(repositoryName)
            .addModifiers(Modifier.PUBLIC)
            .addSuperinterface(ParameterizedTypeName.get(
                ClassName.get(BaseRepository.class),
                ClassName.get(entityElement),
                TypeName.get(primaryKey.element.asType())
            ))
            .addField(createInsertStatement(tableName, fields))
            .addField(createUpdateStatement(tableName, fields, primaryKey))
            .addField(createSelectByIdStatement(tableName, primaryKey))
            .addField(createSelectAllStatement(tableName))
            .addField(createDeleteByIdStatement(tableName, primaryKey))
            .addField(createExistsByIdStatement(tableName, primaryKey))
            .addMethod(createConstructor())
            .addMethod(createInsertStatementMethod(entityElement, fields))
            .addMethod(createInsertMethod(entityElement))
            .addMethod(createSaveStatementMethod(entityElement, fields))
            .addMethod(createSaveMethod(entityElement))
            .addMethod(createUpdateStatementMethod(entityElement, fields, primaryKey))
            .addMethod(createUpdateOneMethod(entityElement))
            .addMethod(createUpdateMethod(entityElement))
            .addMethod(createSelectByIdStatementMethod(entityElement, primaryKey))
            .addMethod(createSelectOneMethod(entityElement, primaryKey))
            .addMethod(createSelectAllStatementMethod(entityElement))
            .addMethod(createSelectMethod(entityElement))
            .addMethod(createDeleteByIdStatementMethod(entityElement, primaryKey))
            .addMethod(createDeleteByIdMethod(entityElement, primaryKey))
            .addMethod(createExistsByIdStatementMethod(entityElement, primaryKey))
            .addMethod(createExistsByIdMethod(entityElement, primaryKey))
            .addMethod(createMapRowMethod(entityElement, fields))
            .build();

        JavaFile javaFile = JavaFile.builder(packageName, repositoryClass)
            .build();

        javaFile.writeTo(processingEnv.getFiler());

        processingEnv.getMessager().printMessage(
            Diagnostic.Kind.NOTE,
            "Generated repository: " + packageName + "." + repositoryName
        );
    }

    private String getRepositoryName(String entityName, GenerateRepository annotation) {
        if (!annotation.repositoryName().isEmpty()) {
            return annotation.repositoryName();
        }

        // Remove "Entity" suffix if present, then add "Base" prefix and "Repository" suffix
        String baseName = entityName.endsWith("Entity")
            ? entityName.substring(0, entityName.length() - 6)
            : entityName;
        return "Base" + baseName + "Repository";
    }

    private String getPackageName(TypeElement entityElement, GenerateRepository annotation) {
        if (!annotation.packageName().isEmpty()) {
            return annotation.packageName();
        }

        String entityPackage = elementUtils.getPackageOf(entityElement).getQualifiedName().toString();

        // Replace ".entity" with ".repository" if present, otherwise append ".repository"
        if (entityPackage.endsWith(".entity")) {
            return entityPackage.substring(0, entityPackage.length() - 7) + ".repository";
        } else {
            return entityPackage + ".repository";
        }
    }

    private String getTableName(String entityName, GenerateRepository annotation) {
        if (!annotation.table().isEmpty()) {
            return annotation.table();
        }

        // Convert EntityName to entity_name (basic snake_case conversion)
        return entityName.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    private FieldSpec createInsertStatement(String tableName, List<FieldElement> fields) {
        List<FieldInfo> insertableFields = getInsertableFields(fields);
        String columnNames = insertableFields.stream()
            .map(field -> field.columnName)
            .collect(Collectors.joining(", "));

        String placeholders = insertableFields.stream()
            .map(field -> "$" + (insertableFields.indexOf(field) + 1))
            .collect(Collectors.joining(", "));

        String sql = String.format(
            "INSERT INTO \"%s\"(%s) VALUES (%s)",
            tableName, columnNames, placeholders
        );

        return FieldSpec.builder(String.class, "INSERT_STATEMENT")
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
            .initializer("$S", sql)
            .build();
    }

    private MethodSpec createConstructor() {
        return MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .build();
    }

    private FieldSpec createUpdateStatement(String tableName, List<FieldElement> fields, FieldInfo primaryKey) {
        List<FieldInfo> updatableFields = getUpdatableFields(fields);

        String setClause = updatableFields.stream()
            .map(field -> field.columnName + " = $" + (updatableFields.indexOf(field) + 1))
            .collect(Collectors.joining(", "));

        String sql = String.format(
            "UPDATE \"%s\" SET %s WHERE %s = $%d",
            tableName, setClause, primaryKey.columnName, updatableFields.size() + 1
        );

        return FieldSpec.builder(String.class, "UPDATE_STATEMENT")
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
            .initializer("$S", sql)
            .build();
    }

    private FieldSpec createSelectByIdStatement(String tableName, FieldInfo primaryKey) {
        return FieldSpec.builder(String.class, "SELECT_BY_ID_STATEMENT")
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
            .initializer("$S", "SELECT * FROM \"" + tableName + "\" WHERE " + primaryKey.columnName + " = $1")
            .build();
    }

    private FieldSpec createSelectAllStatement(String tableName) {
        return FieldSpec.builder(String.class, "SELECT_ALL_STATEMENT")
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
            .initializer("$S", "SELECT * FROM \"" + tableName + "\"")
            .build();
    }

    private FieldSpec createDeleteByIdStatement(String tableName, FieldInfo primaryKey) {
        return FieldSpec.builder(String.class, "DELETE_BY_ID_STATEMENT")
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
            .initializer("$S", "DELETE FROM \"" + tableName + "\" WHERE " + primaryKey.columnName + " = $1")
            .build();
    }

    private FieldSpec createExistsByIdStatement(String tableName, FieldInfo primaryKey) {
        return FieldSpec.builder(String.class, "EXISTS_BY_ID_STATEMENT")
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
            .initializer("$S", "SELECT 1 FROM \"" + tableName + "\" WHERE " + primaryKey.columnName + " = $1 LIMIT 1")
            .build();
    }

    private MethodSpec createInsertStatementMethod(TypeElement entityElement, List<FieldElement> fields) {
        return MethodSpec.methodBuilder("insertStatement")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(Statement.class)
            .addParameter(R2dbcSession.class, "session")
            .addParameter(ClassName.get(entityElement), "entity")
            .addStatement("return saveStatement(session, entity)")
            .build();
    }

    private MethodSpec createInsertMethod(TypeElement entityElement) {
        return MethodSpec.methodBuilder("insert")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(ParameterizedTypeName.get(ClassName.get(CompletionStage.class), ClassName.get(Long.class)))
            .addParameter(R2dbcSession.class, "session")
            .addParameter(ClassName.get(entityElement), "entity")
            .addStatement("return session.updateOne(insertStatement(session, entity))")
            .build();
    }

    private MethodSpec createUpdateStatementMethod(TypeElement entityElement, List<FieldElement> fields, FieldInfo primaryKey) {
        MethodSpec.Builder method = MethodSpec.methodBuilder("updateStatement")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(Statement.class)
            .addParameter(R2dbcSession.class, "session")
            .addParameter(ClassName.get(entityElement), "entity");

        List<FieldInfo> updatableFields = getUpdatableFields(fields);

        StringBuilder statementBuilder = new StringBuilder();
        statementBuilder.append("return session.createStatement(UPDATE_STATEMENT)");

        // Bind updatable fields
        for (int i = 0; i < updatableFields.size(); i++) {
            FieldInfo field = updatableFields.get(i);
            String fieldName = field.element.getSimpleName();
            String accessor = getFieldAccessor(entityElement, fieldName);

            if (isEnumType(field.element)) {
                statementBuilder.append("\n    .bind(").append(i).append(", ").append(accessor).append(".name())");
            } else {
                statementBuilder.append("\n    .bind(").append(i).append(", ").append(accessor).append(")");
            }
        }

        // Bind primary key (last parameter)
        String pkFieldName = primaryKey.element.getSimpleName();
        String pkAccessor = getFieldAccessor(entityElement, pkFieldName);
        statementBuilder.append("\n    .bind(").append(updatableFields.size()).append(", ").append(pkAccessor).append(")");

        method.addCode(statementBuilder.toString() + ";\n");

        return method.build();
    }

    private MethodSpec createUpdateOneMethod(TypeElement entityElement) {
        return MethodSpec.methodBuilder("updateOne")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(ParameterizedTypeName.get(ClassName.get(CompletionStage.class), ClassName.get(Long.class)))
            .addParameter(R2dbcSession.class, "session")
            .addParameter(ClassName.get(entityElement), "entity")
            .addStatement("return session.updateOne(updateStatement(session, entity))")
            .build();
    }

    private MethodSpec createUpdateMethod(TypeElement entityElement) {
        return MethodSpec.methodBuilder("update")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(ParameterizedTypeName.get(
                ClassName.get(CompletionStage.class),
                ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(Long.class))))
            .addParameter(R2dbcSession.class, "session")
            .addParameter(ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(entityElement)), "entities")
            .addStatement("java.util.List<Statement> statements = entities.stream().map(entity -> updateStatement(session, entity)).collect(java.util.stream.Collectors.toList())")
            .addStatement("return session.update(statements)")
            .build();
    }

    private MethodSpec createSelectByIdStatementMethod(TypeElement entityElement, FieldInfo primaryKey) {
        return MethodSpec.methodBuilder("selectByIdStatement")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(Statement.class)
            .addParameter(R2dbcSession.class, "session")
            .addParameter(TypeName.get(primaryKey.element.asType()), "id")
            .addStatement("return session.createStatement(SELECT_BY_ID_STATEMENT).bind(0, id)")
            .build();
    }

    private MethodSpec createSelectOneMethod(TypeElement entityElement, FieldInfo primaryKey) {
        return MethodSpec.methodBuilder("selectOne")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(ParameterizedTypeName.get(
                ClassName.get(CompletionStage.class),
                ParameterizedTypeName.get(ClassName.get(Optional.class), ClassName.get(entityElement))))
            .addParameter(R2dbcSession.class, "session")
            .addParameter(TypeName.get(primaryKey.element.asType()), "id")
            .addStatement("return session.selectOne(selectByIdStatement(session, id), this::mapRow)")
            .build();
    }

    private MethodSpec createSelectAllStatementMethod(TypeElement entityElement) {
        return MethodSpec.methodBuilder("selectAllStatement")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(Statement.class)
            .addParameter(R2dbcSession.class, "session")
            .addStatement("return session.createStatement(SELECT_ALL_STATEMENT)")
            .build();
    }

    private MethodSpec createSelectMethod(TypeElement entityElement) {
        return MethodSpec.methodBuilder("select")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(ParameterizedTypeName.get(
                ClassName.get(CompletionStage.class),
                ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(entityElement))))
            .addParameter(R2dbcSession.class, "session")
            .addStatement("return session.select(selectAllStatement(session), this::mapRow)")
            .build();
    }

    private MethodSpec createMapRowMethod(TypeElement entityElement, List<FieldElement> fields) {
        MethodSpec.Builder method = MethodSpec.methodBuilder("mapRow")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(ClassName.get(entityElement))
            .addParameter(ClassName.get("io.r2dbc.spi", "Row"), "row");

        List<FieldInfo> allFields = fields.stream().map(FieldInfo::new).collect(Collectors.toList());

        if (entityElement.getKind() == ElementKind.RECORD) {
            // Build constructor call with all parameters for records
            StringBuilder constructorCall = new StringBuilder();
            constructorCall.append("return new ").append(entityElement.getSimpleName()).append("(");

            for (int i = 0; i < allFields.size(); i++) {
                FieldInfo field = allFields.get(i);
                String columnName = field.columnName;

                if (i > 0) {
                    constructorCall.append(",\n    ");
                } else {
                    constructorCall.append("\n    ");
                }

                // Generate appropriate row.get() call based on field type
                String getter = generateRowGetter(field, columnName);
                constructorCall.append(getter);
            }

            constructorCall.append("\n)");
            method.addCode(constructorCall.toString() + ";\n");
        } else {
            // For regular classes, create instance and set fields
            method.addStatement("$T instance = new $T()", ClassName.get(entityElement), ClassName.get(entityElement));

            for (FieldInfo field : allFields) {
                String fieldName = field.element.getSimpleName();
                String columnName = field.columnName;
                String getter = generateRowGetter(field, columnName);
                method.addStatement("instance.$L = $L", fieldName, getter);
            }

            method.addStatement("return instance");
        }

        return method.build();
    }

    private String generateRowGetter(FieldInfo field, String columnName) {
        String typeName = field.element.asType().toString();

        // Handle different types
        if (isEnumType(field.element)) {
            // For enums, get as String and convert to enum
            return String.format("%s.valueOf(row.get(\"%s\", String.class))", typeName, columnName);
        } else if (typeName.equals("java.util.UUID")) {
            return String.format("row.get(\"%s\", java.util.UUID.class)", columnName);
        } else if (typeName.equals("java.lang.String")) {
            return String.format("row.get(\"%s\", String.class)", columnName);
        } else if (typeName.equals("java.lang.Long") || typeName.equals("long")) {
            return String.format("row.get(\"%s\", Long.class)", columnName);
        } else if (typeName.equals("java.lang.Integer") || typeName.equals("int")) {
            return String.format("row.get(\"%s\", Integer.class)", columnName);
        } else if (typeName.equals("java.time.LocalDateTime")) {
            return String.format("row.get(\"%s\", java.time.LocalDateTime.class)", columnName);
        } else if (typeName.equals("java.time.LocalDate")) {
            return String.format("row.get(\"%s\", java.time.LocalDate.class)", columnName);
        } else if (typeName.equals("java.lang.Boolean") || typeName.equals("boolean")) {
            return String.format("row.get(\"%s\", Boolean.class)", columnName);
        } else {
            // Default to Object and let R2DBC handle it
            return String.format("(%s) row.get(\"%s\")", typeName, columnName);
        }
    }

    private MethodSpec createSaveStatementMethod(TypeElement entityElement, List<FieldElement> fields) {
        MethodSpec.Builder method = MethodSpec.methodBuilder("saveStatement")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(Statement.class)
            .addParameter(R2dbcSession.class, "session")
            .addParameter(ClassName.get(entityElement), "entity");

        // Build the complete statement in one code block
        StringBuilder statementBuilder = new StringBuilder();
        statementBuilder.append("return session.createStatement(INSERT_STATEMENT)");

        // Get only insertable fields
        List<FieldInfo> insertableFields = getInsertableFields(fields);

        // Add parameter bindings for insertable fields only
        for (int i = 0; i < insertableFields.size(); i++) {
            FieldInfo field = insertableFields.get(i);
            String fieldName = field.element.getSimpleName();
            String accessor = getFieldAccessor(entityElement, fieldName);

            // Handle different types appropriately
            if (isEnumType(field.element)) {
                statementBuilder.append("\n    .bind(").append(i).append(", ").append(accessor).append(".name())");
            } else {
                statementBuilder.append("\n    .bind(").append(i).append(", ").append(accessor).append(")");
            }
        }

        method.addCode(statementBuilder.toString() + ";\n");

        return method.build();
    }

    private MethodSpec createSaveMethod(TypeElement entityElement) {
        return MethodSpec.methodBuilder("save")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(ParameterizedTypeName.get(ClassName.get(CompletionStage.class), ClassName.get(Done.class)))
            .addParameter(R2dbcSession.class, "session")
            .addParameter(ClassName.get(entityElement), "entity")
            .addStatement("return session.updateOne(saveStatement(session, entity)).thenApply(ignored -> Done.getInstance())")
            .build();
    }

    private MethodSpec createDeleteByIdStatementMethod(TypeElement entityElement, FieldInfo primaryKey) {
        return MethodSpec.methodBuilder("deleteByIdStatement")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(Statement.class)
            .addParameter(R2dbcSession.class, "session")
            .addParameter(TypeName.get(primaryKey.element.asType()), "id")
            .addStatement("return session.createStatement(DELETE_BY_ID_STATEMENT).bind(0, id)")
            .build();
    }

    private MethodSpec createDeleteByIdMethod(TypeElement entityElement, FieldInfo primaryKey) {
        return MethodSpec.methodBuilder("deleteById")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(ParameterizedTypeName.get(ClassName.get(CompletionStage.class), ClassName.get(Long.class)))
            .addParameter(R2dbcSession.class, "session")
            .addParameter(TypeName.get(primaryKey.element.asType()), "id")
            .addStatement("return session.updateOne(deleteByIdStatement(session, id))")
            .build();
    }

    private MethodSpec createExistsByIdStatementMethod(TypeElement entityElement, FieldInfo primaryKey) {
        return MethodSpec.methodBuilder("existsByIdStatement")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(Statement.class)
            .addParameter(R2dbcSession.class, "session")
            .addParameter(TypeName.get(primaryKey.element.asType()), "id")
            .addStatement("return session.createStatement(EXISTS_BY_ID_STATEMENT).bind(0, id)")
            .build();
    }

    private MethodSpec createExistsByIdMethod(TypeElement entityElement, FieldInfo primaryKey) {
        return MethodSpec.methodBuilder("existsById")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(ParameterizedTypeName.get(ClassName.get(CompletionStage.class), ClassName.get(Boolean.class)))
            .addParameter(R2dbcSession.class, "session")
            .addParameter(TypeName.get(primaryKey.element.asType()), "id")
            .addStatement("return session.selectOne(existsByIdStatement(session, id), row -> row.get(0, Integer.class) != null)\n    .thenApply(optional -> optional.isPresent())")
            .build();
    }

    private boolean isEnumType(FieldElement field) {
        TypeMirror typeMirror = field.asType();
        Element typeElement = typeUtils.asElement(typeMirror);
        return typeElement != null && typeElement.getKind() == ElementKind.ENUM;
    }

    private String camelToSnakeCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    private String getFieldAccessor(TypeElement entityElement, String fieldName) {
        if (entityElement.getKind() == ElementKind.RECORD) {
            return "entity." + fieldName + "()";
        } else {
            return "entity." + fieldName;
        }
    }

    /**
     * Interface to abstract over RecordComponentElement and VariableElement
     */
    private interface FieldElement {
        String getSimpleName();
        TypeMirror asType();
        <A extends java.lang.annotation.Annotation> A getAnnotation(Class<A> annotationType);
    }

    private static class RecordFieldElement implements FieldElement {
        private final RecordComponentElement element;

        RecordFieldElement(RecordComponentElement element) {
            this.element = element;
        }

        @Override
        public String getSimpleName() {
            return element.getSimpleName().toString();
        }

        @Override
        public TypeMirror asType() {
            return element.asType();
        }

        @Override
        public <A extends java.lang.annotation.Annotation> A getAnnotation(Class<A> annotationType) {
            return element.getAnnotation(annotationType);
        }
    }

    private static class ClassFieldElement implements FieldElement {
        private final VariableElement element;

        ClassFieldElement(VariableElement element) {
            this.element = element;
        }

        @Override
        public String getSimpleName() {
            return element.getSimpleName().toString();
        }

        @Override
        public TypeMirror asType() {
            return element.asType();
        }

        @Override
        public <A extends java.lang.annotation.Annotation> A getAnnotation(Class<A> annotationType) {
            return element.getAnnotation(annotationType);
        }
    }

    /**
     * Helper class to hold field information with Column annotation details.
     */
    private static class FieldInfo {
        final FieldElement element;
        final String columnName;
        final boolean primaryKey;
        final boolean insertable;
        final boolean updatable;
        final boolean nullable;

        FieldInfo(FieldElement element) {
            this.element = element;
            Column column = element.getAnnotation(Column.class);

            if (column != null) {
                this.columnName = column.name().isEmpty() ?
                    camelToSnakeCase(element.getSimpleName()) :
                    column.name();
                this.primaryKey = column.primaryKey();
                this.insertable = column.insertable();
                // If it's explicitly marked as primary key, override the updatable default
                this.updatable = this.primaryKey ? false : column.updatable();
                this.nullable = column.nullable();
            } else {
                this.columnName = camelToSnakeCase(element.getSimpleName());
                this.primaryKey = element.getSimpleName().equals("id"); // Default convention
                this.insertable = true;
                this.updatable = !this.primaryKey; // PKs usually not updatable
                this.nullable = true;
            }
        }

        private static String camelToSnakeCase(String camelCase) {
            return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
        }

        @Override
        public String toString() {
            return String.format("FieldInfo{element=%s, columnName='%s', primaryKey=%s, insertable=%s, updatable=%s}",
                element.getSimpleName(), columnName, primaryKey, insertable, updatable);
        }
    }

    private List<FieldElement> getFields(TypeElement entityElement) {
        if (entityElement.getKind() == ElementKind.RECORD) {
            return entityElement.getRecordComponents()
                .stream()
                .map(RecordFieldElement::new)
                .collect(Collectors.toList());
        } else {
            // For regular classes, get all non-static, non-transient fields
            return entityElement.getEnclosedElements()
                .stream()
                .filter(e -> e.getKind() == ElementKind.FIELD)
                .map(VariableElement.class::cast)
                .filter(field -> !field.getModifiers().contains(Modifier.STATIC))
                .filter(field -> !field.getModifiers().contains(Modifier.TRANSIENT))
                .map(ClassFieldElement::new)
                .collect(Collectors.toList());
        }
    }

    private FieldInfo findPrimaryKey(List<FieldElement> fields) {
        return fields.stream()
            .map(FieldInfo::new)
            .filter(field -> field.primaryKey)
            .findFirst()
            .orElse(new FieldInfo(fields.get(0))); // Default to first field if no PK found
    }

    private List<FieldInfo> getInsertableFields(List<FieldElement> fields) {
        return fields.stream()
            .map(FieldInfo::new)
            .filter(field -> field.insertable)
            .collect(Collectors.toList());
    }

    private List<FieldInfo> getUpdatableFields(List<FieldElement> fields) {
        return fields.stream()
            .map(FieldInfo::new)
            .filter(field -> field.updatable)
            .collect(Collectors.toList());
    }
}
