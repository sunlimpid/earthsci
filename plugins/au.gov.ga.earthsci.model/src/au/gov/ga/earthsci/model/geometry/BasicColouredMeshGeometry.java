package au.gov.ga.earthsci.model.geometry;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import au.gov.ga.earthsci.common.util.AbstractPropertyChangeBean;
import au.gov.ga.earthsci.common.util.MathUtil;
import au.gov.ga.earthsci.common.util.Validate;
import au.gov.ga.earthsci.model.bounds.IBoundingVolume;
import au.gov.ga.earthsci.model.data.IModelData;
import au.gov.ga.earthsci.model.render.IModelGeometryRenderer;

/**
 * A basic implementation of the {@link IMeshGeometry} interface that also
 * implements {@link IVertexColouredGeometry} and allows colour information to
 * be stored at each vertex.
 * 
 * @author James Navin (james.navin@ga.gov.au)
 */
public class BasicColouredMeshGeometry extends AbstractPropertyChangeBean implements IMeshGeometry,
		IVertexColouredGeometry, IMaskedGeometry
{

	private final String id;
	private String name;
	private String description;

	private double opacity = 1.0;

	private IModelData vertices;

	private IModelData mask;
	private boolean zMasked;

	private IModelData normals;

	private IModelData colours;
	private ColourType colourType;

	private IModelData edges;
	private FaceType faceType;

	private IModelGeometryRenderer renderer;

	private IBoundingVolume bounds;

	private final Map<String, IModelData> dataById = new ConcurrentHashMap<String, IModelData>();
	private final Map<String, IModelData> dataByKey = new ConcurrentHashMap<String, IModelData>();

	public BasicColouredMeshGeometry(String id, String name, String description)
	{
		Validate.notBlank(id, "An ID is required"); //$NON-NLS-1$
		this.id = id;
		this.name = name;
		this.description = description;
	}

	@Override
	public String getId()
	{
		return id;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public String getDescription()
	{
		return description;
	}

	@Override
	public IModelData getVertices()
	{
		return vertices;
	}

	public void setVertices(IModelData vertices)
	{
		updateMaps(this.vertices, vertices, VERTICES_KEY);
		firePropertyChange(VERTICES_EVENT_NAME, this.vertices, this.vertices = vertices);
	}

	@Override
	public boolean hasVertices()
	{
		return vertices != null;
	}

	@Override
	public IModelGeometryRenderer getRenderer()
	{
		return renderer;
	}

	@Override
	public void setRenderer(IModelGeometryRenderer renderer)
	{
		firePropertyChange(RENDERER_EVENT_NAME, this.renderer, this.renderer = renderer);
	}

	@Override
	public IModelData getDataById(String id)
	{
		return dataById.get(id);
	}

	@Override
	public IModelData getDataByKey(String key)
	{
		return dataByKey.get(key);
	}

	@Override
	public Set<String> getDataKeys()
	{
		return dataByKey.keySet();
	}

	@Override
	public IModelData getVertexColour()
	{
		return colours;
	}

	public void setVertexColour(IModelData colours)
	{
		updateMaps(this.colours, colours, VERTEX_COLOUR_KEY);
		firePropertyChange(VERTEX_COLOUR_EVENT_NAME, this.colours, this.colours = colours);
	}

	@Override
	public boolean hasVertexColour()
	{
		return colours != null;
	}

	@Override
	public ColourType getColourType()
	{
		return colourType;
	}

	public void setColourType(ColourType colourType)
	{
		firePropertyChange(VERTEX_COLOUR_TYPE_EVENT_NAME, this.colourType, this.colourType = colourType);
	}

	@Override
	public IModelData getEdgeIndices()
	{
		return edges;
	}

	public void setEdgeIndices(IModelData edges)
	{
		updateMaps(this.edges, edges, EDGE_INDICES_KEY);
		firePropertyChange(EDGE_INDICES_EVENT_NAME, this.edges, this.edges = edges);
	}

	@Override
	public boolean hasEdgeIndices()
	{
		return edges != null;
	}

	@Override
	public FaceType getFaceType()
	{
		return faceType;
	}

	public void setFaceType(FaceType faceType)
	{
		firePropertyChange(FACE_TYPE_EVENT_NAME, this.faceType, this.faceType = faceType);
	}

	@Override
	public IModelData getNormals()
	{
		return normals;
	}

	@Override
	public boolean hasNormals()
	{
		return normals != null;
	}

	@Override
	public IBoundingVolume getBoundingVolume()
	{
		return bounds;
	}

	public void setBoundingVolume(IBoundingVolume bounds)
	{
		firePropertyChange(BOUNDING_VOLUME_EVENT_NAME, this.bounds, this.bounds = bounds);
	}

	@Override
	public boolean hasBoundingVolume()
	{
		return bounds != null;
	}

	@Override
	public IModelData getMask()
	{
		return mask;
	}

	public void setMask(IModelData mask)
	{
		updateMaps(this.mask, mask, MASK_KEY);
		firePropertyChange(MASK_EVENT_NAME, this.mask, this.mask = mask);
	}

	@Override
	public boolean hasMask()
	{
		return mask != null;
	}

	public void setUseZMasking(boolean use)
	{
		this.zMasked = use;
	}

	@Override
	public boolean useZMasking()
	{
		return zMasked;
	}

	@Override
	public double getOpacity()
	{
		return this.opacity;
	}

	@Override
	public void setOpacity(double opacity)
	{
		firePropertyChange(OPACITY_EVENT_NAME, this.opacity, this.opacity = MathUtil.clamp(opacity, 0.0, 1.0));
	}

	private void updateMaps(IModelData oldData, IModelData newData, String key)
	{
		removeFromMaps(oldData, key);
		addToMaps(oldData, key);
	}

	private void removeFromMaps(IModelData object, String key)
	{
		if (object == null)
		{
			return;
		}

		dataById.remove(object.getId());
		dataByKey.remove(key);
	}

	private void addToMaps(IModelData object, String key)
	{
		if (object == null)
		{
			return;
		}

		dataById.put(object.getId(), object);
		dataByKey.put(key, object);
	}
}
