package io.serverlessworkflow.fluent.func.serialization.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import io.serverlessworkflow.api.types.CallTask;
import io.serverlessworkflow.api.types.func.CallJava;
import io.serverlessworkflow.api.types.func.CallTaskJava;
import io.serverlessworkflow.api.types.jackson.CallTaskSerializer;

public class CallTaskFunctionSerializer extends CallTaskSerializer {

	@Override
	public void serialize(CallTask value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		
		if (value instanceof CallTaskJava javaCall) {
			
			javaCall.getCallJava();
			
		} else {
			super.serialize(value, gen, serializers);
		}
	}

}
