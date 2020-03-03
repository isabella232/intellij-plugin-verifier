package com.jetbrains.plugin.structure.intellij.beans;

import org.jdom2.Element;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@XmlSeeAlso(ListItemBean.class)
@XmlRootElement(name = "idea-plugin")
public class PluginBean {

  @XmlElement(name = "change-notes") public String changeNotes;
  @XmlElement(name = "description") public String description;
  @XmlElement(name = "name") public String name;
  @XmlElement(name = "id") public String id;
  @XmlAttribute(name = "version") public String formatVersion;
  @XmlElement(name = "version") public String pluginVersion;
  @XmlElement(name = "vendor") public PluginVendorBean vendor;
  @XmlElement(name = "idea-version") public IdeaVersionBean ideaVersion;
  @XmlElement(name = "product-descriptor") public ProductDescriptorBean productDescriptor;
  @XmlElement(name = "is-internal") public boolean isInternal = true;
  @XmlElement(name = "depends") public List<PluginDependencyBean> dependencies = new ArrayList<>();
  @XmlElement(name = "helpset") public List<PluginHelpSetBean> helpSets = new ArrayList<>();
  @XmlElement(name = "category") public String category;
  @XmlElement(name = "resource-bundle") public String resourceBundle;
  @XmlAttribute(name = "url") public String url = "";
  @XmlAttribute(name = "use-idea-classloader") public Boolean useIdeaClassLoader;
  @XmlAttribute(name = "allow-bundled-update") public boolean allowBundledUpdate;

  @XmlElement(name = "module")
  @XmlJavaTypeAdapter(ListItemAdapter.class)
  public List<String> modules = new ArrayList<>();

  @XmlTransient public Map<String, List<Element>> extensions;
  @XmlTransient public List<Element> applicationListeners;
  @XmlTransient public List<Element> projectListeners;

}
