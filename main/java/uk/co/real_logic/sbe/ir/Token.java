/* -*- mode: java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*- */
/*
 * Copyright 2013 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.real_logic.sbe.ir;

import uk.co.real_logic.sbe.PrimitiveType;
import uk.co.real_logic.sbe.util.Verify;

import java.nio.ByteOrder;

/**
 * Class to encapsulate a token of information for the message schema stream. This Intermediate Representation (IR)
 * is intended to be language, schema, platform independent.
 * <p>
 * Processing and optimization could be run over a list of IrNodes to perform various functions
 * <ul>
 *     <li>re-ordering of fields based on size</li>
 *     <li>padding of fields in order to provide expansion room</li>
 *     <li>computing offsets of individual fields</li>
 *     <li>etc.</li>
 * </ul>
 * <p>
 * IR could be used to generate code or other specifications. It should be possible to do the
 * following:
 * <ul>
 *     <li>generate a FIX/SBE schema from IR</li>
 *     <li>generate an ASN.1 spec from IR</li>
 *     <li>generate a GPB spec from IR</li>
 *     <li>etc.</li>
 * </ul>
 * <p>
 * IR could be serialized to storage or network via code generated by SBE. Then read back in to
 * a List of IrNodes.
 * <p>
 * The entire IR of an entity is a {@link java.util.List} of {@link Token} objects. The order of this list is
 * very important. Encoding of fields is done by nodes pointing to specific encoding {@link PrimitiveType}
 * objects. Each encoding node contains size, offset, byte order, and {@link Constraints}. Entities relevant
 * to the encoding such as fields, messages, repeating groups, etc. are encapsulated in the list as nodes
 * themselves. Although, they will in most cases never be serialized. The boundaries of these entities
 * are delimited by BEGIN and END {@link Signal} values in the node {@link Constraints}.
 * A list structure like this allows for each concatenation of encodings as well as easy traversal.
 * <p>
 * An example encoding of a message header might be like this.
 * <ul>
 *     <li>Token 0 - Signal = BEGIN_MESSAGE, schemaId = 100</li>
 *     <li>Token 1 - Signal = BEGIN_FIELD, schemaId = 25</li>
 *     <li>Token 2 - Signal = ENCODING, PrimitiveType = uint32, size = 4, offset = 0</li>
 *     <li>Token 3 - Signal = END_FIELD</li>
 *     <li>Token 4 - Signal = END_MESSAGE</li>
 * </ul>
 * <p>
 */
public class Token
{
    /** Invalid ID value. */
    public static final long INVALID_ID = -1;

    /** Size not determined */
    public static final int VARIABLE_SIZE = -1;

    /** Offset not computed or set */
    public static final int UNKNOWN_OFFSET = -1;

    private final Signal signal;
    private final String name;
    private final long schemaId;
    private final PrimitiveType primitiveType;
    private final int size;
    private final int offset;
    private final ByteOrder byteOrder;
    private final Constraints constraints;

    /**
     * Construct an {@link Token} by providing values for all fields.
     *
     * @param signal        for the token role
     * @param primitiveType representing this node or null.
     * @param size          of the node in bytes.
     * @param offset        within the {@link uk.co.real_logic.sbe.xml.Message}.
     * @param byteOrder     for the encoding.
     * @param constraints      for the {@link uk.co.real_logic.sbe.xml.Message}.
     */
    public Token(final Signal signal,
                 final String name,
                 final long schemaId,
                 final PrimitiveType primitiveType,
                 final int size,
                 final int offset,
                 final ByteOrder byteOrder,
                 final Constraints constraints)
    {
        Verify.notNull(signal, "signal");
        Verify.notNull(name, "name");
        Verify.notNull(primitiveType, "primitiveType");
        Verify.notNull(byteOrder, "byteOrder");
        Verify.notNull(constraints, "constraints");

        this.signal = signal;
        this.name = name;
        this.schemaId = schemaId;
        this.primitiveType = primitiveType;
        this.size = size;
        this.offset = offset;
        this.byteOrder = byteOrder;
        this.constraints = constraints;
    }
    /**
     * Construct a default {@link Token} based on {@link Constraints} with defaults for other fields.
     */
    public Token(final Signal signal, final String name, final long schemaId, final Constraints constraints)
    {
        Verify.notNull(name, "name");
        Verify.notNull(signal, "signal");
        Verify.notNull(constraints, "constraints");

        this.signal = signal;
        this.name = name;
        this.schemaId = schemaId;
        this.primitiveType = null;
        this.size = 0;
        this.offset = 0;
        this.byteOrder = null;
        this.constraints = constraints;
    }

    public Token(final Signal signal, final String name, final long schemaId)
    {
        Verify.notNull(name, "name");
        Verify.notNull(signal, "signal");

        this.signal = signal;
        this.name = name;
        this.schemaId = schemaId;
        this.primitiveType = null;
        this.size = 0;
        this.offset = 0;
        this.byteOrder = null;
        this.constraints = new Constraints();
    }

    /**
     * Signal the role of this token.
     *
     * @return the {@link Signal} for the token.
     */
    public Signal signal()
    {
        return signal;
    }

    /**
     * Return the name of the token
     *
     * @return name of the token
     */
    public String name()
    {
        return name;
    }

    /**
     * Return the ID of the token assigned by the specification
     *
     * @return ID of the token assigned by the specification
     */
    public long schemaId()
    {
        return schemaId;
    }

    /**
     * Get the {@link PrimitiveType} of this field.
     *
     * @return the primitive type of this node. This value is only relevant for nodes that are encodings.
     */
    public PrimitiveType primitiveType()
    {
        return primitiveType;
    }

    /**
     * The size of this token in bytes.
     *
     * @return the size of this node. A value of 0 means the node has no size when encoded. A value of
     *        {@link Token#VARIABLE_SIZE} means this node represents a variable length field.
     */
    public int size()
    {
        return size;
    }

    /**
     * The offset for this token in the message.
     *
     * @return the offset of this Token. A value of 0 means the node has no relevant offset. A value of
     *         {@link Token#UNKNOWN_OFFSET} means this nodes true offset is dependent on variable length
     *         fields ahead of it in the encoding.
     */
    public int offset()
    {
        return offset;
    }

    /**
     * Return the {@link Constraints} of the {@link Token}.
     *
     * @return constraints of the {@link Token}
     */
    public Constraints constraints()
    {
        return constraints;
    }

    /**
     * Return the byte order of this field.
     *
     * @return the byte order for this field.
     */
    public ByteOrder byteOrder()
    {
        return byteOrder;
    }

    public String toString()
    {
        return "Token{" +
            "signal=" + signal +
            ", name='" + name + '\'' +
            ", schemaId=" + schemaId +
            ", primitiveType=" + primitiveType +
            ", size=" + size +
            ", offset=" + offset +
            ", byteOrder=" + byteOrder +
            ", constraints=" + constraints +
            '}';
    }
}
