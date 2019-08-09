package com.basiscomponents.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.basiscomponents.db.util.ResultSetProvider;

public class ResultSetJoinerTest {

	@Test
	public void mostBasicOneResultSetJoinerTest() throws Exception {
		ResultSet rs = ResultSetJoiner.leftJoin(ResultSetProvider.createLeftResultSetForLeftJoinTesting(),
				ResultSetProvider.createRightResultSetForLeftJoinTesting(), "PLZ");
		assertTrue(rs.get(0).getFieldNames().size() == 5);

		assertEquals("Saarbruecken", rs.get(0).getFieldValue("Ort"));
		assertEquals("St. Wendel", rs.get(1).getFieldValue("Ort"));
		assertEquals("Dillingen", rs.get(2).getFieldValue("Ort"));

		assertEquals("Elias", rs.get(0).getFieldValue("Buergermeister"));
		assertEquals("Sascha", rs.get(1).getFieldValue("Buergermeister"));
		assertEquals("Dude", rs.get(2).getFieldValue("Buergermeister"));
	}

	@Test
	public void notAllFieldsResultSetJoinerTest() throws Exception {
		List<String> myList = new ArrayList<String>();
		myList.add("Ort");
		HashMap<ResultSet, List<String>> temp = new HashMap<>();
		temp.put(ResultSetProvider.createRightResultSetForLeftJoinTesting(), myList);
		ResultSet rs = ResultSetJoiner.leftJoin(ResultSetProvider.createLeftResultSetForLeftJoinTesting(),
				temp, "PLZ");
		assertTrue(rs.get(0).getFieldNames().size() == 4);

		assertEquals("Saarbruecken", rs.get(0).getFieldValue("Ort"));
		assertEquals("St. Wendel", rs.get(1).getFieldValue("Ort"));
		assertEquals("Dillingen", rs.get(2).getFieldValue("Ort"));
	}

	@Test
	public void unperfectJoinResultSetJoinerTest() throws Exception {
		DataRow dr = new DataRow();
		dr.setFieldValue("Name", "Hans");
		dr.setFieldValue("Alter", 45);
		dr.setFieldValue("PLZ", "66654");
		ResultSet left = ResultSetProvider.createLeftResultSetForLeftJoinTesting();
		left.add(dr);
		ResultSet rs = ResultSetJoiner.leftJoin(left,
				ResultSetProvider.createRightResultSetForLeftJoinTesting(), "PLZ");
		System.out.println(rs.toJson());
		assertTrue(rs.get(0).getFieldNames().size() == 5);

		assertEquals("Saarbruecken", rs.get(0).getFieldValue("Ort"));
		assertEquals("St. Wendel", rs.get(1).getFieldValue("Ort"));
		assertEquals("Dillingen", rs.get(2).getFieldValue("Ort"));
		
		assertEquals("Elias", rs.get(0).getFieldValue("Buergermeister"));
		assertEquals("Sascha", rs.get(1).getFieldValue("Buergermeister"));
		assertEquals("Dude", rs.get(2).getFieldValue("Buergermeister"));
	}

	@Test
	public void moreComplexResultSetJoinerTest() throws Exception {
		List<ResultSet> temp = new ArrayList<ResultSet>();
		temp.add(ResultSetProvider.createRightResultSetForLeftJoinTesting());
		temp.add(ResultSetProvider.createAnotherRightResultSetForLeftJoinTesting());
		ResultSet rs = ResultSetJoiner.leftJoin(ResultSetProvider.createLeftResultSetForLeftJoinTesting(), temp, "PLZ");
		assertTrue(rs.get(0).getFieldNames().size() == 7);

		assertEquals("Saarbruecken", rs.get(0).getFieldValue("Ort"));
		assertEquals("St. Wendel", rs.get(1).getFieldValue("Ort"));
		assertEquals("Dillingen", rs.get(2).getFieldValue("Ort"));

		assertEquals("Moskau", rs.get(0).getFieldValue("Stadt"));
		assertEquals("Wien", rs.get(1).getFieldValue("Stadt"));
		assertEquals("Konz", rs.get(2).getFieldValue("Stadt"));
	}

}
