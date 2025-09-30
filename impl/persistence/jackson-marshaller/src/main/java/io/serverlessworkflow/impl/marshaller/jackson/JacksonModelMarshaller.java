package io.serverlessworkflow.impl.marshaller.jackson;

import java.io.IOException;
import java.io.UncheckedIOException;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.serverlessworkflow.impl.expressions.jq.JacksonModel;
import io.serverlessworkflow.impl.jackson.JsonUtils;
import io.serverlessworkflow.impl.marshaller.CustomObjectMarshaller;
import io.serverlessworkflow.impl.marshaller.WorkflowInputBuffer;
import io.serverlessworkflow.impl.marshaller.WorkflowOutputBuffer;

public class JacksonModelMarshaller implements CustomObjectMarshaller<JacksonModel> {

	@Override
	public void write(WorkflowOutputBuffer buffer, JacksonModel object) {
		try {
			buffer.writeBytes(JsonUtils.mapper().writeValueAsBytes(object));
		} catch (JsonProcessingException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public JacksonModel read(WorkflowInputBuffer buffer) {
		try {
			return JsonUtils.mapper().readValue(buffer.readBytes(), JacksonModel.class);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

	}

	@Override
	public Class<JacksonModel> getObjectClass() {
		return JacksonModel.class;
	}

}
