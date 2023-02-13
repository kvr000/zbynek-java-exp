package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.router;

import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.container.BeanMethod;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.container.ContainerContext;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.context.RequestExchange;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.path.PathResolver;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.path.JaxRsPathResolver;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.reader.ControllerMeta;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.reader.JaxRsCaptureReader;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.reader.JaxRsCaptureReaderImpl;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.reader.MethodMeta;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.reflect.OwnedMethodId;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.response.ServicerResponse;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.response.NotFoundServicerResponse;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.response.OutputServicerResponse;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.response.UnsupportedRequestServicerResponse;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.router.impl.CallContext;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.router.impl.FunctionData;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


public class JaxRsCaptureServicerRouter implements ServicerRouter
{
	public static JaxRsCaptureServicerRouter fromClasspathJaxRsPaths(Class<?> callingClass, String resource)
	{
		try (
				InputStream stream = Objects.requireNonNull(
						callingClass.getResourceAsStream(resource),
						() -> "Cannot open resource by class "+callingClass+": "+resource);
				JaxRsCaptureReader reader = new JaxRsCaptureReaderImpl(stream)
		) {
			return new JaxRsCaptureServicerRouter(reader.readAll());
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public JaxRsCaptureServicerRouter(List<ControllerMeta> controllers)
	{
		HashMap<String, JaxRsPathResolver.Builder<RequestExchange, FunctionData>> buildersByMethods = new HashMap<>();
		Map<String, List<FunctionData>> anyHandlers = new LinkedHashMap<>();
		JaxRsPathResolver.Builder<RequestExchange, FunctionData> anyBuilder = new JaxRsPathResolver.Builder<>();
		JaxRsPathResolver.Builder<RequestExchange, FunctionData> allBuilder = new JaxRsPathResolver.Builder<>();
		for (ControllerMeta controller: controllers) {
			for (MethodMeta function: controller.getMethods()) {
				String fullPath = anyBuilder.concatPaths(controller.getPath(), function.getPath());
				FunctionData functionData = createFunctionData(controller, function);
				for (String method: function.getMethods()) {
					buildersByMethods.computeIfAbsent(
							method,
							(key) -> {
								JaxRsPathResolver.Builder<RequestExchange, FunctionData> builder = new JaxRsPathResolver.Builder<>();
								for (Map.Entry<String, List<FunctionData>> handler: anyHandlers.entrySet()) {
									for (FunctionData fd: handler.getValue()) {
										builder.registerPath(handler.getKey(), fd);
									}
								}
								return builder;
							}
					)
							.registerPath(fullPath, functionData);
					if (method.equals("*")) {
						anyBuilder.registerPath(fullPath, functionData);
						anyHandlers.computeIfAbsent(fullPath, k -> new ArrayList<>()).add(functionData);
						for (Map.Entry<String, JaxRsPathResolver.Builder<RequestExchange, FunctionData>> entry: buildersByMethods.entrySet()) {
							entry.getValue().registerPath(fullPath, functionData);
						}
					}
				}
				allBuilder.registerPath(fullPath, FunctionData.DUMMY);
			}
		}

		this.controllersByMethods = buildersByMethods.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().build()));
		this.anyControllers = anyBuilder.build();
		this.allControllers = allBuilder.build();
	}

	@Override
	public CompletableFuture<ServicerResponse> call(RequestExchange request, ContainerContext container)
	{
		PathResolver<RequestExchange, FunctionData> controllers = controllersByMethods.getOrDefault(request.getMethod(), anyControllers);
		PathResolver.Match<FunctionData> match = controllers.resolvePath(request.getPath(), request);
		if (match == null || match.handler() == null) {
			if (controllers != anyControllers) {
				match = anyControllers.resolvePath(request.getPath(), request);
			}
			if (match == null || match.handler() == null) {
				match = allControllers.resolvePath(request.getPath(), request);
				if (match != null && match.handler() != null) {
					return CompletableFuture.completedFuture(new UnsupportedRequestServicerResponse());
				}
				return CompletableFuture.completedFuture(new NotFoundServicerResponse());
			}
		}
		CallContext callContext = new CallContext();
		callContext.requestExchange = request;
		callContext.match = match;
		try (BeanMethod<CallContext> methodInvoker = container.resolveMethod(match.handler(), FunctionData::resolverContainerInvoker)) {
			MediaType contentType = match.handler().produces(request);
			return CompletableFuture.completedFuture(new OutputServicerResponse(contentType, methodInvoker.invoke(callContext)));
		}
	}

	private FunctionData createFunctionData(ControllerMeta controller, MethodMeta function)
	{
		return new FunctionData(new OwnedMethodId(JaxRsCaptureServicerRouter.class, controller.getClassName(), function.getFunction()));
	}

	private final Map<String, PathResolver<RequestExchange, FunctionData>> controllersByMethods;

	private final PathResolver<RequestExchange, FunctionData> anyControllers;

	private final PathResolver<RequestExchange, FunctionData> allControllers;
}
