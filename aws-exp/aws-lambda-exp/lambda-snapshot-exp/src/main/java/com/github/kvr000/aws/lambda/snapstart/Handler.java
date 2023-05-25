package com.github.kvr000.aws.lambda.snapstart;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;


public class Handler implements RequestHandler<Void, String>
{
	public String handleRequest(Void input, Context context) {
		return DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneOffset.UTC).format(Instant.now());
	}
}
