# REST + JPA Spatial experiments

Simple REST service storing data into SQL database via JPA (Hibernate).

Three different options were implemented:

## Explicit conversion to JTS type (Place and related classes)

Straightforward solution using ORM supported type in database definition and our friendly type in external model 
objects.  This requires the least upfront effort but **requires conversion from ORM supported type to external type 
everywhere**.

```java
public class PlaceDb
{
	@Column(columnDefinition = "geometry(POINT, 4326)")
	private Point location;
}
```

`Point` is JTS type which is supported natively via Hibernate Spatial extension.


## Hibernate @Type annotation declared on field with transparent converter (Target and related classes)

Using friendly type in both external model and ORM definition.  The friendly type `GeoLocation` is still not 
allowed as query parameter.  This requires creating Hibernate `UserType` which allows **automatic conversion for 
`@Entity` classes but not for other types of queries, including their parameters**.

```java
public class TargetDb
{
	@Type(GeoLocationJpaType.class)
	@Column(columnDefinition = "geometry(POINT, 4326)")
	private GeoLocation location;
}
```

`GeoLocation` is user defined type containing `lon` and `lat` fields.  This class is automatically converted to and 
from JTS `Point` type in `GeoLocationJpaType`.


## Transparent conversion based on type registered via Hibernate TypeContributor (Store and related classes)

Using friendly type in both external model and ORM definition and any other place in the query, including parameters.
This requires a bit more upfront work by creating Hibernate `TypeContributor` but then is **completely transparent 
everywhere**. 

```java
public class StoreDb
{
	@Type(GeoLocationJpaType.class)
	@Column(columnDefinition = "geometry(POINT, 4326)")
	private GeoLocation location;
}
```

`GeoLocation` is user defined type containing `lon` and `lat` fields.  This class is automatically converted to and
from database String containing geometry(point) type dump, based on `GeoLocationTypeContributor` registered as 
META-INF service.