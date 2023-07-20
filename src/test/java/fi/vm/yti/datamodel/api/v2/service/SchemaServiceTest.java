package fi.vm.yti.datamodel.api.v2.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.topbraid.shacl.vocabulary.SH;

import fi.vm.yti.datamodel.api.v2.dto.MSCR;
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
	
	
	private byte[] getByteStreamFromPath(String schemaPath) throws Exception, IOException {
		InputStream inputSchemaInputStream = getClass().getClassLoader().getResourceAsStream(schemaPath);
		byte[] inputSchemaInByte = inputSchemaInputStream.readAllBytes();
		inputSchemaInputStream.close();

		return inputSchemaInByte;
	}	
	
	@Test
	void testSimple1Transformation() throws Exception {
		byte[] data = getByteStreamFromPath("jsonschema/test_jsonschema_valid_simple1.json");
		assertNotNull(data);
		
		String schemaPID = "urn:test:" + UUID.randomUUID().toString();
		Model model = service.transformJSONSchemaToInternal(schemaPID, data);
		
		Resource root = model.createResource(schemaPID + "#root");
		
		assertTrue(model.contains(root, RDF.type, SH.NodeShape));		
		assertEquals(3, model.listSubjectsWithProperty(RDF.type, SH.PropertyShape).toList().size());
		//assertTrue(model.contains(root, SH.closed, model.createTypedLiteral(true)));

	}
	
	@Test
	void testSimpleNested() throws Exception {
		byte[] data = getByteStreamFromPath("jsonschema/test_jsonschema_valid_simple_nested.json");
		assertNotNull(data);
		
		String schemaPID = "urn:test:" + UUID.randomUUID().toString();
		Model model = service.transformJSONSchemaToInternal(schemaPID, data);
		
		Resource root = model.createResource(schemaPID + "#root");
		
		assertTrue(model.contains(root, RDF.type, SH.NodeShape));		
		assertEquals(3, model.listSubjectsWithProperty(RDF.type, SH.NodeShape).toList().size());
		assertEquals(7, model.listSubjectsWithProperty(RDF.type, SH.PropertyShape).toList().size());
		
		assertEquals(SH.NodeShape, model.getRequiredProperty(model.createResource(schemaPID + "#root/address"), RDF.type).getObject());
		
		//assertTrue(model.contains(root, SH.closed, model.createTypedLiteral(true)));
		
	}
	
	@Test
	void testValidDatatypes() throws Exception {
		byte[] data = getByteStreamFromPath("jsonschema/test_jsonschema_valid_simple_datatypes.json");
		assertNotNull(data);
		
		String schemaPID = "urn:test:" + UUID.randomUUID().toString();
		Model model = service.transformJSONSchemaToInternal(schemaPID, data);
		assertEquals(XSD.integer, model.getRequiredProperty(model.createResource(schemaPID + "#root/address/house_number"), SH.datatype).getObject());
		assertEquals(XSD.integer, model.getRequiredProperty(model.createResource(schemaPID + "#root/address/city/population"), SH.datatype).getObject());
		assertEquals(XSD.xfloat, model.getRequiredProperty(model.createResource(schemaPID + "#root/height"), SH.datatype).getObject());
		assertEquals(XSD.xboolean, model.getRequiredProperty(model.createResource(schemaPID + "#root/has_cats"), SH.datatype).getObject());
		assertEquals("common", model.getRequiredProperty(model.createResource(schemaPID + "#root/lastName"), SH.defaultValue).getString());
		assertEquals(MSCR.NULL, model.getRequiredProperty(model.createResource(schemaPID + "#root/address"), SH.defaultValue).getObject());
	}
	
	@Test
	void testValidRequired() throws Exception {
		byte[] data = getByteStreamFromPath("jsonschema/test_jsonschema_valid_required.json");
		assertNotNull(data);
		
		String schemaPID = "urn:test:" + UUID.randomUUID().toString();
		Model model = service.transformJSONSchemaToInternal(schemaPID, data);
		assertEquals(1, model.getRequiredProperty(model.createResource(schemaPID + "#root/lastName"), SH.minCount).getInt());
		assertEquals(1, model.getRequiredProperty(model.createResource(schemaPID + "#root/lastName"), SH.maxCount).getInt());
		
	}
	
	@Test
	void testValidArrays() throws Exception {
		byte[] data = getByteStreamFromPath("jsonschema/test_jsonschema_valid_arrays.json");
		assertNotNull(data);
		
		String schemaPID = "urn:test:" + UUID.randomUUID().toString();
		Model model = service.transformJSONSchemaToInternal(schemaPID, data);

		assertEquals(XSD.xstring, model.getRequiredProperty(model.createResource(schemaPID + "#root/firstName"), SH.datatype).getObject());
		// lastName is functional property -> must have maxCount = 1
		assertEquals(1, model.getRequiredProperty(model.createResource(schemaPID + "#root/firstName"), SH.maxCount).getInt());
		// lastName is not required -> should not have minCount
		assertFalse(model.contains(model.createResource(schemaPID + "#root/firstName"), SH.minCount));

		// not restrictions on number of items in an array -> no maxCount
		assertFalse(model.contains(model.createResource(schemaPID + "#root/lastNames"), SH.maxCount));

		assertEquals(2, model.getRequiredProperty(model.createResource(schemaPID + "#root/addresses/numbers"), SH.minCount).getInt());
		assertFalse(model.contains(model.createResource(schemaPID + "#root/addresses/numbers"), SH.maxCount));

		assertEquals(10, model.getRequiredProperty(model.createResource(schemaPID + "#root/addresses/city/area_codes"), SH.maxCount).getInt());
		assertEquals(1, model.getRequiredProperty(model.createResource(schemaPID + "#root/addresses/city/area_codes"), SH.minCount).getInt());

		
	}
	
	/*
	@Test
	void testTransforJSONSchemaToInternal() throws Exception, IOException {
		InputStream inputStream = SchemaServiceTest.class.getResourceAsStream("jsonschemas/test_jsonschema_valid_s.json");
		assertNotNull(inputStream);
		
		String schemaPID = "urn:test:" + UUID.randomUUID().toString();
		Model model = service.transformJSONSchemaToInternal(schemaPID, inputStream.readAllBytes());
		
		model.write(System.out, "TURTLE");
		
	}
	*/
	
	
}

