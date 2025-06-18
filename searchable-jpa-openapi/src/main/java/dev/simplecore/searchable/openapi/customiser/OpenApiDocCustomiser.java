package dev.simplecore.searchable.openapi.customiser;

import dev.simplecore.searchable.core.condition.SearchCondition;
import dev.simplecore.searchable.openapi.annotation.SearchableParams;
import dev.simplecore.searchable.openapi.generator.DescriptionGenerator;
import dev.simplecore.searchable.openapi.generator.ExampleGenerator;
import dev.simplecore.searchable.openapi.generator.ParameterGenerator;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Component
public class OpenApiDocCustomiser implements OpenApiCustomiser {
    private static final Logger log = LoggerFactory.getLogger(OpenApiDocCustomiser.class);
    private static final String MEDIA_TYPE_JSON = "application/json";

    private final RequestMappingHandlerMapping handlerMapping;
    private final ExampleGenerator exampleGenerator;
    private final ParameterGenerator parameterGenerator;

    public OpenApiDocCustomiser(@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
        this.exampleGenerator = new ExampleGenerator();
        this.parameterGenerator = new ParameterGenerator();
    }

    @Override
    public void customise(OpenAPI openApi) {
        log.info("Customizing OpenAPI documentation for search conditions");
        initializeOpenApiPaths(openApi);
        customizeHandlerMethods(openApi);
    }

    private void initializeOpenApiPaths(OpenAPI openApi) {
        if (openApi.getPaths() == null) {
            log.debug("OpenAPI paths is null, initializing new Paths");
            openApi.setPaths(new io.swagger.v3.oas.models.Paths());
        }
    }

    private void customizeHandlerMethods(OpenAPI openApi) {
        handlerMapping.getHandlerMethods().forEach((requestMapping, handlerMethod) -> {
            try {
                customizeSearchCondition(openApi, requestMapping, handlerMethod);
            } catch (Exception e) {
                log.error("Error customizing search condition for handler method: {}", handlerMethod, e);
            }
        });
    }

    private void customizeSearchCondition(OpenAPI openApi, RequestMappingInfo requestMapping,
                                          HandlerMethod handlerMethod) {
        Set<String> patterns = requestMapping.getPatternValues();
        if (patterns.isEmpty()) return;

        patterns.forEach(pattern -> {
            PathItem pathItem = openApi.getPaths().get(pattern);
            if (pathItem == null) return;

            Operation operation = getOperation(pathItem, requestMapping);
            if (operation == null) return;

            processSearchConditionParameter(handlerMethod, operation, pattern);
        });
    }

    private void processSearchConditionParameter(HandlerMethod handlerMethod, Operation operation, String pattern) {
        Arrays.stream(handlerMethod.getMethodParameters())
                .filter(this::isSearchConditionParameter)
                .findFirst()
                .ifPresent(param -> customizeOperation(param, operation, pattern));
    }

    private boolean isSearchConditionParameter(MethodParameter param) {
        Class<?> parameterType = param.getParameterType();
        return SearchCondition.class.isAssignableFrom(parameterType) ||
                param.hasParameterAnnotation(SearchableParams.class);
    }

    private void customizeOperation(MethodParameter param, Operation operation, String pattern) {
        Class<?> dtoClass = extractDtoClass(param);
        if (dtoClass == null) return;

        boolean isPostType = isPostTypeParameter(param);

        DescriptionGenerator.customizeOperation(
                operation,
                dtoClass,
                pattern,
                isPostType ? DescriptionGenerator.RequestType.POST : DescriptionGenerator.RequestType.GET
        );

        if (isPostType) {
            customizeRequestBody(operation, dtoClass);
        } else {
            parameterGenerator.customizeParameters(operation, dtoClass);
        }
    }

    private boolean isPostTypeParameter(MethodParameter param) {
        return SearchCondition.class.isAssignableFrom(param.getParameterType());
    }

    private Class<?> extractDtoClass(MethodParameter param) {
        if (param.hasParameterAnnotation(SearchableParams.class)) {
            SearchableParams annotation = param.getParameterAnnotation(SearchableParams.class);
            return annotation != null ? annotation.value() : null;
        }
        return (Class<?>) ((ParameterizedType) param.getGenericParameterType()).getActualTypeArguments()[0];
    }

    private Operation getOperation(PathItem pathItem, RequestMappingInfo requestMapping) {
        Set<RequestMethod> methods = requestMapping.getMethodsCondition().getMethods();
        if (methods.contains(RequestMethod.GET))
            return pathItem.getGet();
        if (methods.contains(RequestMethod.POST))
            return pathItem.getPost();
        if (methods.contains(RequestMethod.PUT))
            return pathItem.getPut();
        if (methods.contains(RequestMethod.DELETE))
            return pathItem.getDelete();
        return null;
    }

    private void customizeRequestBody(Operation operation, Class<?> dtoClass) {
        try {
            RequestBody requestBody = new RequestBody();
            Content content = new Content();
            MediaType mediaType = new MediaType();

            // Generate schema automatically using ModelConverters
            Schema<?> schema;
            java.lang.reflect.Type parameterizedType = new ParameterizedType() {
                @Override
                public java.lang.reflect.Type[] getActualTypeArguments() {
                    return new java.lang.reflect.Type[]{dtoClass};
                }

                @Override
                public java.lang.reflect.Type getRawType() {
                    return SearchCondition.class;
                }

                @Override
                public java.lang.reflect.Type getOwnerType() {
                    return null;
                }
            };

            schema = ModelConverters.getInstance().resolveAsResolvedSchema(
                    new AnnotatedType(parameterizedType)
                            .resolveAsRef(false)
                            .skipSchemaName(true)
            ).schema;

            Map<String, Example> examples = new LinkedHashMap<>();

            String simpleExample = exampleGenerator.generateSimpleExample(dtoClass);
            Example simple = new Example();
            simple.setValue(simpleExample);
            simple.setDescription("Simple example with one filter");
            simple.setSummary("Simple Example");
            examples.put("simple", simple);

            Example complete = new Example();
            complete.setValue(exampleGenerator.generateCompleteExample(dtoClass));
            complete.setDescription("Complete example with multiple filters");
            complete.setSummary("Complete Example");
            examples.put("complete", complete);

            mediaType.schema(schema);
            mediaType.examples(examples);
            content.addMediaType(MEDIA_TYPE_JSON, mediaType);
            requestBody.content(content);
            requestBody.required(true);
            operation.requestBody(requestBody);
        } catch (Exception e) {
            log.error("Failed to customize request body", e);
        }
    }
}