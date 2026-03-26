package com.local.caejobservice.adapter.infrastructure;

import com.local.caejobservice.adapter.domain.CaeAdapter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.springframework.stereotype.Component;

@Component
public class AdapterRegistry {

  private final Map<String, CaeAdapter> adapters;

  public AdapterRegistry(List<CaeAdapter> adapters) {
    Map<String, CaeAdapter> adapterMap = new TreeMap<>();
    for (CaeAdapter adapter : adapters) {
      adapterMap.put(adapter.toolName(), adapter);
    }
    this.adapters = Map.copyOf(adapterMap);
  }

  public Optional<CaeAdapter> get(String toolName) {
    return Optional.ofNullable(adapters.get(toolName));
  }

  public List<String> supportedTools() {
    return adapters.keySet().stream().sorted().toList();
  }
}
