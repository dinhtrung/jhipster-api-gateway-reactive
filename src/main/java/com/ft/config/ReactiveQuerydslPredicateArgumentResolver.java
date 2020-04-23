package com.ft.config;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.querydsl.binding.QuerydslBindingsFactory;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.data.querydsl.binding.QuerydslPredicateBuilder;
import org.springframework.data.util.CastUtils;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.data.web.querydsl.QuerydslPredicateArgumentResolver;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;

public class ReactiveQuerydslPredicateArgumentResolver implements SyncHandlerMethodArgumentResolver {


	private static final ResolvableType PREDICATE = ResolvableType.forClass(Predicate.class);
	private static final ResolvableType OPTIONAL_OF_PREDICATE = ResolvableType.forClassWithGenerics(Optional.class,
			PREDICATE);

	private final QuerydslBindingsFactory bindingsFactory;
	private final QuerydslPredicateBuilder predicateBuilder;

	/**
	 * Creates a new {@link QuerydslPredicateArgumentResolver} using the given {@link ConversionService}.
	 *
	 * @param factory
	 * @param conversionService defaults to {@link DefaultConversionService} if {@literal null}.
	 */
	public ReactiveQuerydslPredicateArgumentResolver() {
		this.bindingsFactory  =  new QuerydslBindingsFactory(SimpleEntityPathResolver.INSTANCE);
		this.predicateBuilder = new QuerydslPredicateBuilder(DefaultConversionService.getSharedInstance(),
				bindingsFactory.getEntityPathResolver());
	}
	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#supportsParameter(org.springframework.core.MethodParameter)
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {

		ResolvableType type = ResolvableType.forMethodParameter(parameter);

		if (PREDICATE.isAssignableFrom(type) || OPTIONAL_OF_PREDICATE.isAssignableFrom(type)) {
			return true;
		}

		if (parameter.hasParameterAnnotation(QuerydslPredicate.class)) {
			throw new IllegalArgumentException(String.format("Parameter at position %s must be of type Predicate but was %s.",
					parameter.getParameterIndex(), parameter.getParameterType()));
		}

		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#resolveArgument(org.springframework.core.MethodParameter, org.springframework.web.method.support.ModelAndViewContainer, org.springframework.web.context.request.NativeWebRequest, org.springframework.web.bind.support.WebDataBinderFactory)
	 */
	@Override
	public Object resolveArgumentValue(MethodParameter parameter, BindingContext bindingContext,
			ServerWebExchange exchange) {

		MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();

		for (Entry<String, List<String>> entry : exchange.getRequest().getQueryParams().entrySet()) {
			parameters.put(entry.getKey(), entry.getValue());
		}

		Optional<QuerydslPredicate> annotation = Optional
				.ofNullable(parameter.getParameterAnnotation(QuerydslPredicate.class));
		TypeInformation<?> domainType = extractTypeInfo(parameter).getRequiredActualType();

		Optional<Class<? extends QuerydslBinderCustomizer<?>>> bindingsAnnotation = annotation //
				.map(QuerydslPredicate::bindings) //
				.map(CastUtils::cast);

		QuerydslBindings bindings = bindingsAnnotation //
				.map(it -> bindingsFactory.createBindingsFor(domainType, it)) //
				.orElseGet(() -> bindingsFactory.createBindingsFor(domainType));

		Predicate result = predicateBuilder.getPredicate(domainType, parameters, bindings);

		if (!parameter.isOptional() && result == null) {
			return new BooleanBuilder();
		}

		return OPTIONAL_OF_PREDICATE.isAssignableFrom(ResolvableType.forMethodParameter(parameter)) //
				? Optional.ofNullable(result) //
				: result;
	}

	/**
	 * Obtains the domain type information from the given method parameter. Will favor an explicitly registered on through
	 * {@link QuerydslPredicate#root()} but use the actual type of the method's return type as fallback.
	 *
	 * @param parameter must not be {@literal null}.
	 * @return
	 */
	static TypeInformation<?> extractTypeInfo(MethodParameter parameter) {

		Optional<QuerydslPredicate> annotation = Optional
				.ofNullable(parameter.getParameterAnnotation(QuerydslPredicate.class));

		return annotation.filter(it -> !Object.class.equals(it.root()))//
				.<TypeInformation<?>> map(it -> ClassTypeInformation.from(it.root()))//
				.orElseGet(() -> detectDomainType(parameter));
	}

	private static TypeInformation<?> detectDomainType(MethodParameter parameter) {

		Method method = parameter.getMethod();

		if (method == null) {
			throw new IllegalArgumentException("Method parameter is not backed by a method!");
		}

		return detectDomainType(ClassTypeInformation.fromReturnTypeOf(method));
	}

	private static TypeInformation<?> detectDomainType(TypeInformation<?> source) {

		if (source.getTypeArguments().isEmpty()) {
			return source;
		}

		TypeInformation<?> actualType = source.getActualType();

		if (actualType == null) {
			throw new IllegalArgumentException(String.format("Could not determine domain type from %s!", source));
		}

		if (source != actualType) {
			return detectDomainType(actualType);
		}

		if (source instanceof Iterable) {
			return source;
		}

		return detectDomainType(source.getRequiredComponentType());
	}
}
