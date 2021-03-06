/*******************************************************************************
 * Copyright 2012 Geoscience Australia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package au.gov.ga.earthsci.core.model.layer;

import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.terrain.CompoundElevationModel;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;

import au.gov.ga.earthsci.common.collection.HashSetHashMap;
import au.gov.ga.earthsci.common.collection.SetMap;
import au.gov.ga.earthsci.common.util.IEnableable;
import au.gov.ga.earthsci.common.util.IInformationed;
import au.gov.ga.earthsci.core.model.IModelStatus;
import au.gov.ga.earthsci.core.model.ModelStatus;
import au.gov.ga.earthsci.core.persistence.Exportable;
import au.gov.ga.earthsci.core.persistence.Persistent;
import au.gov.ga.earthsci.core.tree.AbstractTreeNode;
import au.gov.ga.earthsci.core.worldwind.WorldWindCompoundElevationModel;

/**
 * Abstract implementation of the {@link ILayerTreeNode} interface.
 * 
 * @author Michael de Hoog (michael.dehoog@ga.gov.au)
 */
@Exportable
public abstract class AbstractLayerTreeNode extends AbstractTreeNode<ILayerTreeNode> implements ILayerTreeNode
{
	private String name;
	private LayerList layerList;
	private WorldWindCompoundElevationModel elevationModels;
	private SetMap<URI, ILayerTreeNode> uriMap;
	private boolean lastAnyChildrenEnabled, lastAllChildrenEnabled;
	private String label;
	private URI uri;
	private IContentType contentType;
	private URL nodeInformationURL;
	private URL legendURL;
	private URL iconURL;
	private boolean expanded;
	private final Object semaphore = new Object();
	private IModelStatus status = ModelStatus.ok(null);

	protected AbstractLayerTreeNode()
	{
		super(ILayerTreeNode.class);
		addPropertyChangeListener("enabled", new EnabledChangeListener()); //$NON-NLS-1$
	}

	@Persistent(attribute = true)
	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public void setName(String name)
	{
		firePropertyChange("name", getName(), this.name = name); //$NON-NLS-1$
	}

	@Persistent(attribute = true)
	@Override
	public String getLabel()
	{
		return label;
	}

	@Override
	public void setLabel(String label)
	{
		if (getName() != null && getName().equals(label))
		{
			setLabel(null);
			return;
		}
		firePropertyChange("label", getLabel(), this.label = label); //$NON-NLS-1$
	}

	@Override
	public String getLabelOrName()
	{
		return getLabel() == null ? getName() : getLabel();
	}

	@Persistent(name = "uri")
	@Override
	public URI getURI()
	{
		return uri;
	}

	@Override
	public void setURI(URI uri)
	{
		firePropertyChange("uRI", getURI(), this.uri = uri); //$NON-NLS-1$
	}

	@Override
	public IContentType getContentType()
	{
		return contentType;
	}

	@Override
	public void setContentType(IContentType contentType)
	{
		firePropertyChange("contentType", getContentType(), this.contentType = contentType); //$NON-NLS-1$
	}

	@Persistent
	public String getContentTypeId()
	{
		return contentType == null ? null : contentType.getId();
	}

	public void setContentTypeId(String contentTypeId)
	{
		contentType = contentTypeId == null ? null : Platform.getContentTypeManager().getContentType(contentTypeId);
	}

	/**
	 * The URL pointing to this node's information page.
	 * <p/>
	 * If this node is a layer node and the associated layer implements
	 * {@link IInformationed}, then the layer's information URL should be used
	 * for information instead of this one.
	 * 
	 * @return The URL pointing to this node's information page.
	 */
	@Persistent(name = "infoURL")
	public URL getNodeInformationURL()
	{
		return nodeInformationURL;
	}

	public void setNodeInformationURL(URL nodeInformationURL)
	{
		firePropertyChange("nodeInformationURL", getNodeInformationURL(), this.nodeInformationURL = nodeInformationURL); //$NON-NLS-1$
	}

	@Override
	public URL getInformationURL()
	{
		return getNodeInformationURL();
	}

	@Override
	public String getInformationString()
	{
		return LayerNodeDescriber.describe(this);
	}

	@Persistent
	@Override
	public URL getLegendURL()
	{
		return legendURL;
	}

	public void setLegendURL(URL legendURL)
	{
		firePropertyChange("legendURL", getLegendURL(), this.legendURL = legendURL); //$NON-NLS-1$
	}

	@Persistent
	@Override
	public URL getIconURL()
	{
		return iconURL;
	}

	public void setIconURL(URL iconURL)
	{
		firePropertyChange("iconURL", getIconURL(), this.iconURL = iconURL); //$NON-NLS-1$
	}

	@Persistent(name = "children")
	public ILayerTreeNode[] getChildrenAsArray()
	{
		List<ILayerTreeNode> children = getChildren();
		return getChildren().toArray(new ILayerTreeNode[children.size()]);
	}

	public void setChildrenAsArray(ILayerTreeNode[] children)
	{
		setChildren(Arrays.asList(children));
	}

	@Override
	public boolean isAnyChildrenEnabled()
	{
		return anyChildrenEnabledEquals(true);
	}

	@Override
	public boolean isAllChildrenEnabled()
	{
		return !anyChildrenEnabledEquals(false);
	}

	@Override
	public boolean anyChildrenEnabledEquals(boolean enabled)
	{
		if (this instanceof IEnableable)
		{
			IEnableable enableable = (IEnableable) this;
			if (enableable.isEnabled() == enabled)
			{
				return true;
			}
		}
		if (hasChildren())
		{
			for (ILayerTreeNode child : getChildren())
			{
				if (child.anyChildrenEnabledEquals(enabled))
				{
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void enableChildren(boolean enabled)
	{
		if (this instanceof IEnableable)
		{
			IEnableable enableable = (IEnableable) this;
			enableable.setEnabled(enabled);
		}
		if (hasChildren())
		{
			for (ILayerTreeNode child : getChildren())
			{
				child.enableChildren(enabled);
			}
		}
	}

	@Override
	public LayerList getLayerList()
	{
		synchronized (semaphore)
		{
			if (layerList == null)
			{
				layerList = new LayerList();
				updateLayerList();
			}
			return layerList;
		}
	}

	private void updateLayerList()
	{
		synchronized (semaphore)
		{
			if (layerList != null)
			{
				layerList.removeAll();
				addNodesToLayerList(this);
			}
		}
	}

	private void addNodesToLayerList(ILayerTreeNode node)
	{
		if (node instanceof Layer)
		{
			layerList.add((Layer) node);
		}
		for (ILayerTreeNode child : node.getChildren())
		{
			addNodesToLayerList(child);
		}
	}

	@Override
	public boolean hasNodesForURI(URI uri)
	{
		synchronized (semaphore)
		{
			if (uriMap == null)
			{
				uriMap = new HashSetHashMap<URI, ILayerTreeNode>();
				updateURIMap();
			}
			return uriMap.containsKey(uri);
		}
	}

	@Override
	public ILayerTreeNode[] getNodesForURI(URI uri)
	{
		synchronized (semaphore)
		{
			if (uriMap == null)
			{
				uriMap = new HashSetHashMap<URI, ILayerTreeNode>();
				updateURIMap();
			}
			Set<ILayerTreeNode> nodes = uriMap.get(uri);
			if (nodes != null)
			{
				return nodes.toArray(new ILayerTreeNode[nodes.size()]);
			}
			return new ILayerTreeNode[0];
		}
	}

	@Override
	public CompoundElevationModel getElevationModels()
	{
		synchronized (semaphore)
		{
			if (elevationModels == null)
			{
				elevationModels = new WorldWindCompoundElevationModel();
				updateElevationModels();
			}
			return elevationModels;
		}
	}

	protected void updateElevationModels()
	{
		synchronized (semaphore)
		{
			if (elevationModels != null)
			{
				elevationModels.removeAll();
				addNodesToElevationModels(this);
			}
		}
	}

	private void addNodesToElevationModels(ILayerTreeNode node)
	{
		if (node instanceof LayerNode)
		{
			LayerNode layerNode = (LayerNode) node;
			if (layerNode.getLayer() instanceof IElevationModelLayer)
			{
				IElevationModelLayer elevationModelLayer = (IElevationModelLayer) layerNode.getLayer();
				elevationModels.addElevationModel(elevationModelLayer.getElevationModel());
			}
		}
		for (ILayerTreeNode child : node.getChildren())
		{
			addNodesToElevationModels(child);
		}
	}

	private void updateURIMap()
	{
		synchronized (semaphore)
		{
			if (uriMap != null)
			{
				uriMap.clear();
				addNodesToURIMap(this);
			}
		}
	}

	private void addNodesToURIMap(ILayerTreeNode node)
	{
		uriMap.putSingle(node.getURI(), node);
		for (ILayerTreeNode child : node.getChildren())
		{
			addNodesToURIMap(child);
		}
	}

	@Override
	protected void fireChildrenPropertyChange(List<ILayerTreeNode> oldChildren, List<ILayerTreeNode> newChildren)
	{
		childrenChanged(oldChildren, children);
		super.fireChildrenPropertyChange(oldChildren, newChildren);
	}

	@Override
	public void childrenChanged(List<ILayerTreeNode> oldChildren, List<ILayerTreeNode> newChildren)
	{
		//TODO should we implement a (more efficient?) modification of these collections according to changed children?
		//update the collections if they exist
		updateLayerList();
		updateElevationModels();
		updateURIMap();

		//fire property changes
		fireAnyAllChildrenEnabledChanged();

		//recurse up to the root node
		if (!isRoot())
		{
			getParent().childrenChanged(oldChildren, newChildren);
		}
	}

	@Override
	public void enabledChanged()
	{
		//fire property changes
		fireAnyAllChildrenEnabledChanged();

		//recurse up to the root node
		if (!isRoot())
		{
			getParent().enabledChanged();
		}
	}

	private void fireAnyAllChildrenEnabledChanged()
	{
		firePropertyChange(
				"allChildrenEnabled", lastAllChildrenEnabled, lastAllChildrenEnabled = isAllChildrenEnabled()); //$NON-NLS-1$
		firePropertyChange(
				"anyChildrenEnabled", lastAnyChildrenEnabled, lastAnyChildrenEnabled = isAnyChildrenEnabled()); //$NON-NLS-1$
	}

	private class EnabledChangeListener implements PropertyChangeListener
	{
		@Override
		public void propertyChange(PropertyChangeEvent evt)
		{
			enabledChanged();
		}
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + " {" + getLabelOrName() + "}"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Persistent(attribute = true)
	@Override
	public boolean isExpanded()
	{
		return expanded;
	}

	@Override
	public void setExpanded(boolean expanded)
	{
		firePropertyChange("expanded", isExpanded(), this.expanded = expanded); //$NON-NLS-1$
	}

	@Override
	public IModelStatus getStatus()
	{
		return status;
	}

	@Override
	public void setStatus(IModelStatus status)
	{
		firePropertyChange("status", getStatus(), this.status = status); //$NON-NLS-1$
	}
}
