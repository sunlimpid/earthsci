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
package au.gov.ga.earthsci.catalog.part;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.graphics.Image;

import au.gov.ga.earthsci.application.IFireableLabelProvider;
import au.gov.ga.earthsci.application.IconLoader;
import au.gov.ga.earthsci.application.ImageRegistry;
import au.gov.ga.earthsci.core.model.catalog.ICatalogModel;
import au.gov.ga.earthsci.core.model.catalog.ICatalogTreeNode;
import au.gov.ga.earthsci.core.model.layer.ILayerTreeNode;
import au.gov.ga.earthsci.core.tree.ITreeNode;
import au.gov.ga.earthsci.core.util.ILabeled;
import au.gov.ga.earthsci.core.util.SetMap;
import au.gov.ga.earthsci.core.worldwind.ITreeModel;
import au.gov.ga.earthsci.viewers.IControlProvider;

/**
 * A {@link IControlProvider} for the catalog browser tree
 * 
 * @author James Navin (james.navin@ga.gov.au)
 */
@Creatable
public class CatalogTreeLabelProvider extends LabelProvider implements ILabelDecorator, IFireableLabelProvider
{
	private final org.eclipse.jface.resource.ImageRegistry decoratedImageCache =
			new org.eclipse.jface.resource.ImageRegistry();

	@Inject
	private ICatalogBrowserController controller;

	private IconLoader iconLoader = new IconLoader(this);

	@Inject
	private ICatalogModel catalogModel;

	@Inject
	private ITreeModel layerModel;

	private boolean disposed = false;

	@PostConstruct
	public void postConstruct()
	{
		setupListeners();
		addListeners();
	}

	@PreDestroy
	public void preDestroy()
	{
		removeListeners();
	}

	@Override
	public Image getImage(Object element)
	{
		if (!(element instanceof ICatalogTreeNode))
		{
			return null;
		}
		ICatalogTreeNode node = (ICatalogTreeNode) element;

		URL iconURL = CatalogTreeLabelProviderRegistry.getProvider(node).getIconURL(node);
		return getImage(element, iconURL);
	}

	@Override
	public String getText(Object element)
	{
		if (!(element instanceof ICatalogTreeNode))
		{
			if (element instanceof ILabeled)
			{
				return ((ILabeled) element).getLabelOrName();
			}
		}
		ICatalogTreeNode node = (ICatalogTreeNode) element;
		return CatalogTreeLabelProviderRegistry.getProvider(node).getLabel(node);
	}

	@Override
	public void dispose()
	{
		if (disposed)
			return;
		disposed = true;

		super.dispose();
		decoratedImageCache.dispose();
		iconLoader.dispose();

		//TODO we probably want to call dispose on the CatalogTreeNodeControlProviderRegistry at some point
		//but maybe not here because it feels wrong to dispose of a static factory's resources in a
		//non-static context (ie every time the catalog part is closed).
	}

	@Override
	public Image decorateImage(Image image, Object element)
	{
		if (!(element instanceof ICatalogTreeNode) || !((ICatalogTreeNode) element).isLayerNode())
		{
			return null;
		}

		ICatalogTreeNode node = (ICatalogTreeNode) element;

		if (!controller.existsInLayerModel(node.getLayerURI()))
		{
			return null;
		}

		return getDecoratedIcon(image);
	}

	@Override
	public String decorateText(String text, Object element)
	{
		if (!(element instanceof ICatalogTreeNode) || !((ICatalogTreeNode) element).isLayerNode())
		{
			return null;
		}

		ICatalogTreeNode node = (ICatalogTreeNode) element;

		if (!controller.existsInLayerModel(node.getLayerURI()))
		{
			return null;
		}
		return text + "*"; //$NON-NLS-1$
	}

	private Image getImage(Object element, URL imageURL)
	{
		if (imageURL == null)
		{
			return null;
		}

		return iconLoader.getImage(element, imageURL);
	}

	private Image getDecoratedIcon(Image base)
	{
		String key = base.hashCode() + ""; //$NON-NLS-1$

		if (base.isDisposed())
		{
			decoratedImageCache.remove(key);
			return null;
		}

		Image decorated = decoratedImageCache.get(key);
		if (decorated != null)
		{
			return decorated;
		}

		decorated =
				new DecorationOverlayIcon(base, ImageRegistry.getInstance().getDescriptor(
						ImageRegistry.DECORATION_INCLUDED), IDecoration.BOTTOM_RIGHT).createImage();
		decoratedImageCache.put(key, decorated);
		return decorated;
	}

	@Override
	public void fireLabelProviderChanged(LabelProviderChangedEvent event)
	{
		super.fireLabelProviderChanged(event);
	}

	private PropertyChangeListener catalogModelChildrenListener;
	private PropertyChangeListener catalogModelURIListener;
	private PropertyChangeListener layerModelChildrenListener;
	private PropertyChangeListener layerModelURIListener;

	private void setupListeners()
	{
		final SetMap<URI, ICatalogTreeNode> uriElements = new SetMap<URI, ICatalogTreeNode>();

		catalogModelChildrenListener = new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				ITreeNode<?>[] oldChildren = (ITreeNode<?>[]) evt.getOldValue();
				ITreeNode<?>[] newChildren = (ITreeNode<?>[]) evt.getNewValue();
				addOrRemoveNodes(oldChildren, false);
				addOrRemoveNodes(newChildren, true);
			}

			private void addOrRemoveNodes(ITreeNode<?>[] nodes, boolean add)
			{
				if (nodes != null)
				{
					for (ITreeNode<?> n : nodes)
					{
						Object value = n.getValue();
						if (value instanceof ICatalogTreeNode)
						{
							ICatalogTreeNode node = (ICatalogTreeNode) value;
							URI uri = node.getLayerURI();
							if (uri != null)
							{
								if (add)
									uriElements.putSingle(uri, node);
								else
									uriElements.removeSingle(uri, node);
							}
						}
					}
				}
			}
		};

		catalogModelURIListener = new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				Object source = evt.getSource();
				if (source instanceof ICatalogTreeNode)
				{
					ICatalogTreeNode node = (ICatalogTreeNode) source;
					URI oldURI = (URI) evt.getOldValue();
					URI newURI = (URI) evt.getNewValue();
					if (oldURI != null)
					{
						uriElements.removeSingle(oldURI, node);
					}
					if (newURI != null)
					{
						uriElements.putSingle(newURI, node);
					}
				}
			}
		};

		layerModelChildrenListener = new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				Set<URI> changedURIs = new HashSet<URI>();
				ITreeNode<?>[] oldChildren = (ITreeNode<?>[]) evt.getOldValue();
				ITreeNode<?>[] newChildren = (ITreeNode<?>[]) evt.getNewValue();
				addURIsToSet(oldChildren, changedURIs);
				addURIsToSet(newChildren, changedURIs);
				updateElementsForURIs(changedURIs, uriElements);
			}

			private void addURIsToSet(ITreeNode<?>[] nodes, Set<URI> list)
			{
				if (nodes != null)
				{
					for (ITreeNode<?> n : nodes)
					{
						Object value = n.getValue();
						if (value instanceof ILayerTreeNode)
						{
							ILayerTreeNode node = (ILayerTreeNode) value;
							URI uri = node.getURI();
							if (uri != null)
							{
								list.add(uri);
							}
						}
					}
				}
			}
		};

		layerModelURIListener = new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				URI oldURI = (URI) evt.getOldValue();
				URI newURI = (URI) evt.getNewValue();
				Set<URI> uris = new HashSet<URI>();
				if (oldURI != null)
				{
					uris.add(oldURI);
				}
				if (newURI != null)
				{
					uris.add(newURI);
				}
				updateElementsForURIs(uris, uriElements);
			}
		};
	}

	private void updateElementsForURIs(Collection<URI> uris, SetMap<URI, ICatalogTreeNode> uriElements)
	{
		Set<ICatalogTreeNode> elements = new HashSet<ICatalogTreeNode>();
		for (URI uri : uris)
		{
			Set<ICatalogTreeNode> nodes = uriElements.get(uri);
			if (nodes != null)
			{
				elements.addAll(nodes);
			}
		}
		Object[] elementsArray = elements.toArray();
		fireLabelProviderChanged(new LabelProviderChangedEvent(CatalogTreeLabelProvider.this, elementsArray));
	}

	private void addListeners()
	{
		catalogModel.getRoot().addDescendantPropertyChangeListener("children", catalogModelChildrenListener); //$NON-NLS-1$
		catalogModel.getRoot().addDescendantPropertyChangeListener("layerURI", catalogModelURIListener); //$NON-NLS-1$
		layerModel.getRootNode().addDescendantPropertyChangeListener("children", layerModelChildrenListener); //$NON-NLS-1$
		layerModel.getRootNode().addDescendantPropertyChangeListener("uRI", layerModelURIListener); //$NON-NLS-1$
	}

	private void removeListeners()
	{
		catalogModel.getRoot().removePropertyChangeListener("children", catalogModelChildrenListener); //$NON-NLS-1$
		catalogModel.getRoot().removePropertyChangeListener("layerURI", catalogModelURIListener); //$NON-NLS-1$
		layerModel.getRootNode().removePropertyChangeListener("children", layerModelChildrenListener); //$NON-NLS-1$
		layerModel.getRootNode().removePropertyChangeListener("uRI", layerModelURIListener); //$NON-NLS-1$
	}
}