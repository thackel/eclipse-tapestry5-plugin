package com.anjlab.eclipse.tapestry5.views;

import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;

import com.anjlab.eclipse.tapestry5.LibraryMapping;
import com.anjlab.eclipse.tapestry5.TapestryModule;

public class ViewLabelDecorator extends LabelProvider implements ILightweightLabelDecorator
{

    @Override
    public void decorate(Object element, IDecoration decoration)
    {
        if (element instanceof TreeObject)
        {
            Object data = ((TreeObject) element).getData();
            
            if (data instanceof TapestryModule)
            {
                decoration.addSuffix(" " + ((TapestryModule) data).getReference().getLabel());
            }
            else if (data instanceof LibraryMapping)
            {
                decoration.addSuffix(" " + ((LibraryMapping) data).getRootPackage());
            }
            else if (data instanceof Throwable)
            {
                decoration.addSuffix(" " + ((Throwable) data).getMessage());
            }
            else if (data instanceof String)
            {
                decoration.addSuffix(" " + data);
            }
        }
    }

}
