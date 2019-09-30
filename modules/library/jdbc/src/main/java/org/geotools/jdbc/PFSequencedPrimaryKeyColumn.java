/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2019, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */

package org.geotools.jdbc;

/**
 * Represents a column of a primary key which has an associated pre-fetch sequence used to generate
 * its values. Different from the SequencedPrimaryKeyColumn, the value for this column will be
 * fetched from a sequence before the inserting in the data store.
 *
 * <p>New values for the key are generated by selecting the next value of the sequence.
 *
 * @author Paulo Sergio SAMPAIO de ARAGAO, TCS
 * @source $URL$
 */
public class PFSequencedPrimaryKeyColumn extends PrimaryKeyColumn {

    String sequenceName;

    /**
     * @param name : name of column in table/view
     * @param type : data type of columns
     * @param sequenceName : name of Sequence object in database to generate keys
     */
    public PFSequencedPrimaryKeyColumn(String name, Class type, String sequenceName) {
        super(name, type);
        this.sequenceName = sequenceName;
    }

    /** @return the sequenceName */
    public String getSequenceName() {
        return sequenceName;
    }
}
