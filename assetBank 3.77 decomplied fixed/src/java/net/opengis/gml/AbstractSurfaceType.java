package net.opengis.gml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="AbstractSurfaceType")
@XmlSeeAlso({OrientableSurfaceType.class, PolygonType.class, SurfaceType.class})
public class AbstractSurfaceType extends AbstractGeometricPrimitiveType
{
}

/* Location:           D:\Project - Photint Asset-bank\net\opengis\gml.zip
 * Qualified Name:     gml.AbstractSurfaceType
 * JD-Core Version:    0.6.0
 */