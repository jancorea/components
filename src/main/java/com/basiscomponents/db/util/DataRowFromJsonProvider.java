package com.basiscomponents.db.util;

import java.sql.Types;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.basiscomponents.db.BBArrayList;
import com.basiscomponents.db.DataField;
import com.basiscomponents.db.DataRow;
import com.basiscomponents.db.ResultSet;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DataRowFromJsonProvider {

	private static final String COLUMN_TYPE = "ColumnType";

	@SuppressWarnings("unchecked")
	public static DataRow fromJson(final String in, final DataRow ar) throws Exception {
		String input = in;
		if (input.length() < 2) {
			return new DataRow();
		}
		DataRow attributes;
		if (ar == null) {
			attributes = new DataRow();
		} else {
			attributes = ar.clone();
		}

		// convert characters below chr(32) to \\uxxxx notation
		int i = 0;

		while (i < input.length()) {
			if (input.charAt(i) < 31) {
				String hex = String.format("%04x", (int) input.charAt(i));
				input = input.substring(0, i) + "\\u" + hex + input.substring(i + 1);
			}
			i++;
		}

		if (input.startsWith("{\"datarow\":[") && input.endsWith("]}")) {
			input = input.substring(11, input.length() - 1);
		}
		String intmp = input;
		JsonNode root = new ObjectMapper().readTree(intmp);

		if (input.startsWith("{") && input.endsWith("}")) {
			input = "[" + input + "]";
		}
		JsonFactory f = new JsonFactory();
		JsonParser jp = f.createParser(input);
		jp.nextToken();
		ObjectMapper objectMapper = new ObjectMapper();

		List<?> navigation = objectMapper.readValue(jp,
				objectMapper.getTypeFactory().constructCollectionType(List.class, Object.class));

		if (navigation.isEmpty()) {
			return new DataRow();
		}

		HashMap<?, ?> hm = (HashMap<?, ?>) navigation.get(0);

		DataRow dr = new DataRow();

		if (hm.containsKey("meta")) {
			// new format
			HashMap<?, ?> meta = (HashMap<?, ?>) hm.get("meta");
			if (meta == null)
				meta = new HashMap();
			
			Iterator<?> it = hm.keySet().iterator();
			while (it.hasNext()) {
				String fieldName = (String) it.next();
				
				@SuppressWarnings("unchecked")
				HashMap<String, ?> fieldMeta = ((HashMap) meta.get(fieldName));
				if (!attributes.contains(fieldName) && !fieldName.equals("meta")) {
					String s = "12";

					if (fieldMeta == null) {
						fieldMeta = new HashMap<>();
					}


					if (fieldMeta.get(COLUMN_TYPE) != null)
						s = (String) fieldMeta.get(COLUMN_TYPE);
					if (s != null) {
						attributes.addDataField(fieldName, Integer.parseInt(s), new DataField(null));
						Set<String> ks = fieldMeta.keySet();
						if (ks.size() > 1) {
							Iterator<String> itm = ks.iterator();
							while (itm.hasNext()) {
								String k = itm.next();
								if (k.equals(COLUMN_TYPE))
									continue;
								attributes.setFieldAttribute((String) fieldName, k, (String) fieldMeta.get(k));
							}
						}
					} // if s!=null
				}
			}
		}

		// add all fields to the attributes record that were not part of it before
		Iterator<?> it2 = hm.keySet().iterator();
		while (it2.hasNext()) {
			String fieldName = (String) it2.next();
			if (!attributes.contains(fieldName) && !fieldName.equals("meta") && root.get(fieldName) != null) {
				switch (root.get(fieldName).getNodeType().toString()) {
				case "NUMBER":
					attributes.addDataField(fieldName, java.sql.Types.DOUBLE, new DataField(null));
					break;
				case "BOOLEAN":
					attributes.addDataField(fieldName, java.sql.Types.BOOLEAN, new DataField(null));
					break;
				default:
					attributes.addDataField(fieldName, java.sql.Types.VARCHAR, new DataField(null));
					break;

				}
			}
		}

		if (!attributes.isEmpty()) {
			BBArrayList<String> names = attributes.getFieldNames();

			Iterator<?> it = names.iterator();

			while (it.hasNext()) {
				String fieldName = (String) it.next();

				Object fieldObj = hm.get(fieldName);
				int fieldType = attributes.getFieldType(fieldName);
				if (fieldObj == null) {
					dr.addDataField(fieldName, fieldType, new DataField(null));
					dr.setFieldAttributes(fieldName, attributes.getFieldAttributes(fieldName));
					continue;
				}
				switch (fieldType) {
				case -974:
					// nested DataRow
					System.err.println("fromJson not implemented for nested DataRow!");
					// FIXME: need to parse and create the nested ResultSet as well
					// this is https://github.com/BasisHub/components/issues/115
					dr.setFieldValue(fieldName, new DataRow());
					break;					
				case -975:
					// nested ResultSet
					System.err.println("fromJson not implemented for nested ResultSet!");
					// FIXME: need to parse and create the nested ResultSet as well
					// this is https://github.com/BasisHub/components/issues/115
					dr.setFieldValue(fieldName, new ResultSet());
					break;
				
				case java.sql.Types.CHAR:
				case java.sql.Types.VARCHAR:
				case java.sql.Types.NVARCHAR:
				case java.sql.Types.NCHAR:
				case java.sql.Types.LONGVARCHAR:
				case java.sql.Types.LONGNVARCHAR:
					// got a JSON object - save it as a JSON String
					if (fieldObj.getClass().equals(java.util.LinkedHashMap.class)) {
						dr.addDataField(fieldName, fieldType, new DataField(root.get(fieldName).toString()));
						dr.setFieldAttribute(fieldName, "StringFormat", "JSON");
					} else
						dr.addDataField(fieldName, fieldType, new DataField(fieldObj));
					break;
				case java.sql.Types.BIGINT:
				case java.sql.Types.TINYINT:
				case java.sql.Types.INTEGER:
				case java.sql.Types.SMALLINT:
					String tmp = fieldObj.toString();
					if (tmp.isEmpty())
						tmp = "0";
					dr.addDataField(fieldName, fieldType, new DataField(Integer.parseInt(tmp)));
					break;
				case java.sql.Types.NUMERIC:
					dr.addDataField(fieldName, fieldType, new DataField(new java.math.BigDecimal(fieldObj.toString())));
					break;
				case java.sql.Types.DOUBLE:
				case java.sql.Types.FLOAT:
				case java.sql.Types.DECIMAL:
				case java.sql.Types.REAL:
					dr.addDataField(fieldName, fieldType, new DataField(Double.parseDouble(fieldObj.toString())));
					break;
				case java.sql.Types.BOOLEAN:
				case java.sql.Types.BIT:
					dr.addDataField(fieldName, fieldType, new DataField(fieldObj));
					break;
				case java.sql.Types.TIMESTAMP:
				case java.sql.Types.TIMESTAMP_WITH_TIMEZONE:
				case (int) 11:
					String tss = fieldObj.toString();
					if (!tss.contains("T")) {
						tss+="T00:00:00.0";
					}
					java.sql.Timestamp ts = (java.sql.Timestamp) DataField.convertType(tss, fieldType);
					dr.addDataField(fieldName, fieldType, new DataField(ts));
					break;
				case java.sql.Types.DATE:
				case (int) 9:
					tss = fieldObj.toString();
					dr.addDataField(fieldName, fieldType,
							new DataField((java.sql.Date) DataField.convertType(tss, fieldType)));
					break;
				case java.sql.Types.ARRAY:
				case java.sql.Types.BINARY:
				case java.sql.Types.BLOB:
				case java.sql.Types.CLOB:
				case java.sql.Types.DATALINK:
				case java.sql.Types.DISTINCT:
				case java.sql.Types.JAVA_OBJECT:
				case java.sql.Types.LONGVARBINARY:
				case java.sql.Types.NCLOB:
				case java.sql.Types.NULL:
				case java.sql.Types.OTHER:
				case java.sql.Types.REF:
				case java.sql.Types.REF_CURSOR:
				case java.sql.Types.ROWID:
				case java.sql.Types.SQLXML:
				case java.sql.Types.STRUCT:
				case java.sql.Types.TIME:
				case java.sql.Types.TIME_WITH_TIMEZONE:
				case java.sql.Types.VARBINARY:
				default:
					break;

				}// switch

				Map<String, String> attr = attributes.getFieldAttributes(fieldName);


				@SuppressWarnings("unchecked")
				HashMap<String, HashMap> m = (HashMap<String, HashMap>) hm.get("meta");
				if (m != null && m.containsKey(fieldName)) {
					attr.putAll((HashMap<String, String>) m.get(fieldName));
					dr.setFieldAttributes(fieldName, attr);
				}
			}
		} else {
			// old format - deprecated
			Iterator<?> it = navigation.iterator();
			while (it.hasNext()) {
				@SuppressWarnings("unchecked")
				HashMap<String, ?> field = (HashMap<String, ?>) it.next();
				String name = (String) field.get("Name");
				String type = (String) field.get("Type");
				if (type == null)
					continue;
				switch (type) {
				case "C":
					String strval = (String) field.get("StringValue");
					if (strval == null)
						strval = "";
					dr.setFieldValue(name, strval);
					break;
				case "N":
					Object o = field.get("NumericValue");
					Double numval;
					if (o == null)
						numval = 0.0;
					else
						numval = Double.parseDouble(o.toString());
					dr.setFieldValue(name, numval);
					break;
				case "D":
					Object d = field.get("DateValue");
					if (d == null) {
						dr.setFieldValue(name, Types.DATE, null);
					} else {
						dr.setFieldValue(name, Types.DATE, d.toString());
					}
					break;
				case "T":
					Object t = field.get("TimestampValue");
					if (t == null)
						dr.setFieldValue(name, Types.TIMESTAMP, null);
					else
						dr.setFieldValue(name, Types.TIMESTAMP, t.toString());
					break;
				default:
					break;
				}
			}
		}
		return dr;
	}
}
;