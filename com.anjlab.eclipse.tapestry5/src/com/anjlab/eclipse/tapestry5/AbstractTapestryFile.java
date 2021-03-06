package com.anjlab.eclipse.tapestry5;

public abstract class AbstractTapestryFile implements TapestryFile
{
    @Override
    public boolean isTemplateFile()
    {
        return TapestryUtils.isTemplateFile(getPath());
    }

    @Override
    public boolean isJavaFile()
    {
        return TapestryUtils.isJavaFile(getPath());
    }

    @Override
    public boolean isPropertiesFile()
    {
        return TapestryUtils.isPropertiesFile(getPath());
    }

    @Override
    public boolean isJavaScriptFile()
    {
        return TapestryUtils.isJavaScriptFile(getPath());
    }

    @Override
    public boolean isStyleSheetFile()
    {
        return TapestryUtils.isStyleSheetFile(getPath());
    }

    @Override
    public String getFileExtension()
    {
        return getPath().getFileExtension();
    }

}