/*
 * Copyright 2020-Present The Serverless Workflow Specification Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.serverlessworkflow.generator.jackson;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.Module.SetupContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JPackage;
import io.github.classgraph.AnnotationClassRef;
import io.github.classgraph.AnnotationInfo;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassRefTypeSignature;
import io.github.classgraph.MethodInfo;
import io.github.classgraph.ScanResult;
import io.github.classgraph.TypeArgument;
import io.github.classgraph.TypeSignature;
import io.serverlessworkflow.annotations.AdditionalProperties;
import io.serverlessworkflow.annotations.Item;
import io.serverlessworkflow.annotations.ItemKey;
import io.serverlessworkflow.annotations.ItemValue;
import io.serverlessworkflow.annotations.Union;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(
    name = "generate",
    defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    requiresDependencyResolution = ResolutionScope.COMPILE,
    threadSafe = true)
public class JacksonMixInPojo extends AbstractMojo {

  @Parameter(
      property = "jacksonmixinpojo.outputDirectory",
      defaultValue = "${project.build.directory}/generated-sources/jacksonmixinpojo")
  private File outputDirectory;

  /**
   * Package name used for generated Java classes (for types where a fully qualified name has not
   * been supplied in the schema using the 'javaType' property).
   *
   * @since 0.1.0
   */
  @Parameter(property = "jsonschema2pojo.targetPackage")
  private String targetPackage = "";

  private static final String MIXIN_METHOD = "setMixInAnnotation";
  private static final String ADD_PROPERTIES_METHOD = "getAdditionalProperties";
  private static final String SETUP_METHOD = "setupModule";
  private JCodeModel codeModel;
  private JPackage rootPackage;
  private JMethod setupMethod;

  @FunctionalInterface
  interface AnnotationProcessor {
    void accept(ClassInfo classInfo, JDefinedClass definedClass)
        throws JClassAlreadyExistsException;
  }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {

    try (ScanResult result =
        new ClassGraph()
            .enableAnnotationInfo()
            .enableMethodInfo()
            .acceptPackages(targetPackage)
            .scan()) {
      codeModel = new JCodeModel();
      rootPackage = codeModel._package(targetPackage);
      setupMethod =
          rootPackage
              ._class("JacksonMixInModule")
              ._extends(SimpleModule.class)
              .method(JMod.PUBLIC, codeModel.VOID, SETUP_METHOD);
      processAnnotatedClasses(result, Union.class, this::buildUnionMixIn);
      processAnnotatedClasses(result, AdditionalProperties.class, this::buildAdditionalPropsMixIn);
      processAnnotatedClasses(result, Item.class, this::buildItemMixIn);
      processAnnotatedClasses(result.getAllEnums(), this::buildEnumMixIn);
      setupMethod
          .body()
          .invoke(JExpr._super(), SETUP_METHOD)
          .arg(setupMethod.param(SetupContext.class, "context"));
      Files.createDirectories(outputDirectory.toPath());
      codeModel.build(outputDirectory, (PrintStream) null);
    } catch (JClassAlreadyExistsException | IOException e) {
      getLog().error(e);
    }
  }

  private void processAnnotatedClasses(
      Iterable<ClassInfo> classesInfo, AnnotationProcessor processor)
      throws JClassAlreadyExistsException {
    for (ClassInfo classInfo : classesInfo) {
      setupMethod
          .body()
          .invoke(JExpr._super(), MIXIN_METHOD)
          .arg(JExpr.dotclass(codeModel.ref(classInfo.getName())))
          .arg(processAnnotatedClass(classInfo, processor));
    }
  }

  private void processAnnotatedClasses(
      ScanResult result, Class<? extends Annotation> annotation, AnnotationProcessor processor)
      throws JClassAlreadyExistsException {
    processAnnotatedClasses(result.getClassesWithAnnotation(annotation), processor);
  }

  private JExpression processAnnotatedClass(ClassInfo classInfo, AnnotationProcessor processor)
      throws JClassAlreadyExistsException {
    JDefinedClass result = createMixInClass(classInfo);
    processor.accept(classInfo, result);
    return JExpr.dotclass(result);
  }

  private void buildAdditionalPropsMixIn(
      ClassInfo addPropsClassInfo, JDefinedClass addPropsMixClass) {
    JClass mapStringObject =
        getReturnType(addPropsClassInfo.getMethodInfo().getSingleMethod(ADD_PROPERTIES_METHOD));
    JClass objectType = mapStringObject.getTypeParameters().get(1);
    addPropsMixClass
        .method(JMod.ABSTRACT, mapStringObject, ADD_PROPERTIES_METHOD)
        .annotate(JsonAnyGetter.class);
    addPropsMixClass
        .field(JMod.NONE, mapStringObject, "additionalProperties")
        .annotate(JsonIgnore.class);
    JMethod setter =
        addPropsMixClass.method(JMod.ABSTRACT, codeModel.VOID, "setAdditionalProperty");
    setter.param(String.class, "name");
    setter.param(objectType, "value");
    setter.annotate(JsonAnySetter.class);
  }

  private void buildItemMixIn(ClassInfo classInfo, JDefinedClass mixClass)
      throws JClassAlreadyExistsException {
    JClass relClass = codeModel.ref(classInfo.getName());
    MethodInfo keyMethod =
        classInfo.getMethodInfo().filter(m -> m.hasAnnotation(ItemKey.class)).get(0);
    MethodInfo valueMethod =
        classInfo.getMethodInfo().filter(m -> m.hasAnnotation(ItemValue.class)).get(0);
    mixClass
        .annotate(JsonSerialize.class)
        .param(
            "using",
            GeneratorUtils.generateSerializer(
                relClass, keyMethod.getName(), valueMethod.getName()));
    mixClass
        .annotate(JsonDeserialize.class)
        .param("using", GeneratorUtils.generateDeserializer(relClass, getReturnType(valueMethod)));
  }

  private void buildUnionMixIn(ClassInfo unionClassInfo, JDefinedClass unionMixClass)
      throws JClassAlreadyExistsException {
    JClass unionClass = codeModel.ref(unionClassInfo.getName());
    unionMixClass
        .annotate(JsonSerialize.class)
        .param("using", GeneratorUtils.generateSerializer(unionClass));
    unionMixClass
        .annotate(JsonDeserialize.class)
        .param(
            "using",
            GeneratorUtils.generateDeserializer(unionClass, getUnionClasses(unionClassInfo)));
  }

  private void buildEnumMixIn(ClassInfo classInfo, JDefinedClass mixClass)
      throws JClassAlreadyExistsException {
    mixClass.method(JMod.ABSTRACT, String.class, "value").annotate(JsonValue.class);

    JMethod staticMethod =
        mixClass.method(JMod.STATIC, codeModel.ref(classInfo.getName()), "fromValue");
    staticMethod.param(String.class, "value");
    staticMethod.annotate(JsonCreator.class);
    staticMethod.body()._return(JExpr._null());
  }

  private JDefinedClass createMixInClass(ClassInfo classInfo) throws JClassAlreadyExistsException {
    return rootPackage._class(JMod.ABSTRACT, classInfo.getSimpleName() + "MixIn");
  }

  private Collection<JClass> getUnionClasses(ClassInfo unionClassInfo) {
    AnnotationInfo info = unionClassInfo.getAnnotationInfoRepeatable(Union.class).get(0);
    Object[] unionClasses = (Object[]) info.getParameterValues().getValue("value");
    return Stream.of(unionClasses)
        .map(AnnotationClassRef.class::cast)
        .map(ref -> codeModel.ref(ref.getName()))
        .collect(Collectors.toList());
  }

  private JClass getReturnType(MethodInfo info) {
    return getReturnType(info.getTypeSignatureOrTypeDescriptor().getResultType());
  }

  private JClass getReturnType(ClassRefTypeSignature refTypeSignature) {
    JClass result = codeModel.ref(refTypeSignature.getFullyQualifiedClassName());
    for (TypeArgument t : refTypeSignature.getTypeArguments())
      result = result.narrow(getReturnType(t.getTypeSignature()));
    return result;
  }

  private JClass getReturnType(TypeSignature t) {
    return t instanceof ClassRefTypeSignature refTypeSignature
        ? getReturnType(refTypeSignature)
        : codeModel.ref(t.toString());
  }
}
