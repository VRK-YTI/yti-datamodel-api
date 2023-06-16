package fi.vm.yti.datamodel.api.v2.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import fi.vm.yti.datamodel.api.v2.mapper.ClassMapper;
import fi.vm.yti.datamodel.api.v2.mapper.ResourceMapper;

@ExtendWith(SpringExtension.class)
@Import({
	SchemaService.class,
	ClassMapper.class,
	ResourceMapper.class,
	JenaService.class
})
public class SchemaServiceTest {

	@Autowired
	private SchemaService service;
	
	
	@Test
	void testTransforJSONSchemaToInternal() throws Exception, IOException {
		InputStream inputStream = SchemaServiceTest.class.getResourceAsStream("/test_json_trimmed.json");
//		var inputStream = getClass().getResourceAsStream("test_json_trimmed.json");
//		var inputStream = getClass().getResourceAsStream("test_jsonschema_b2share.json");
//		InputStream inputStream = getClass().getResourceAsStream("test_jsonschema_b2share.json");
//		InputStream inputStream = new FileInputStream("src/test/resources/test_jsonschema_b2share.json");
//		InputStream inputStream = new FileInputStream("src/test/resources/test_json_trimmed.json");
//		var inputStream = getClass().getResourceAsStream("/test_jsonschema_invalid_schema.json");
//		var inputStream = getClass().getResourceAsStream("/test_jsonschema_invalid_schema_no_enum.json");
		assertNotNull(inputStream);
		
		String schemaPID = "urn:test:" + UUID.randomUUID().toString();
		Model model = service.transformJSONSchemaToInternal(schemaPID, inputStream.readAllBytes());
		
		model.write(System.out, "TURTLE");
		
	}
}

