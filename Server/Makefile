# Author: danlp6
# Server housekeeping. Run from Server/ or with `make -C Server <target>`.
# Assets/ is a plain folder (the CDN root): point python -m http.server / nginx / Tomcat at it.

PYTHON     ?= python3
ASSET_ROOT ?= Assets
ASSET_DEFS ?= assets.defs.json
MANIFEST   ?= assets.manifest
SRC        ?=
PORT       ?= 8080

ASSETS := $(PYTHON) assets.py --repo . --defs $(ASSET_DEFS) --root $(ASSET_ROOT) --manifest $(MANIFEST)

# Everything the manifest is derived from, read from the defs (enum files, CardMeta.json, plugin jars + their folder)
DEF_SOURCES := $(shell $(ASSETS) sources)

.PHONY: help asset-paths asset-list asset-tree asset-check asset-clean asset-gather asset-plugins asset-index asset-update asset-serve

help:
	@echo "asset-paths   derive expected asset paths -> $(MANIFEST)"
	@echo "asset-list    list files present under $(ASSET_ROOT)"
	@echo "asset-tree    create folder structure (PLACEHOLDERS=1 for empty files)"
	@echo "asset-check   diff expected vs. present (STRICT=1 fails on missing)"
	@echo "asset-clean   remove orphans/empty dirs (dry run; FORCE=1, PRUNE_EMPTY=1)"
	@echo "asset-gather  copy source assets in (SRC=<dir>)"
	@echo "asset-plugins extract assets shipped in Backend/data/plugins/*.jar"
	@echo "asset-index   write $(ASSET_ROOT)/index.json with sizes + sha256"
	@echo "asset-update  paths -> clean -> tree -> plugins -> index"
	@echo "asset-serve   serve $(ASSET_ROOT) on :$(PORT) for local dev"

$(MANIFEST): $(DEF_SOURCES)
	@$(ASSETS) paths

asset-paths:
	@$(ASSETS) paths

asset-list:
	@$(ASSETS) list

asset-tree: $(MANIFEST)
	@$(ASSETS) tree $(if $(PLACEHOLDERS),--placeholders)

asset-check: $(MANIFEST)
	@$(ASSETS) check $(if $(STRICT),--strict)

asset-clean: $(MANIFEST)
	@$(ASSETS) clean $(if $(FORCE),--force) $(if $(PRUNE_EMPTY),--prune-empty)

asset-gather: $(MANIFEST)
	@$(ASSETS) gather --src "$(SRC)"

asset-plugins: $(MANIFEST)
	@$(ASSETS) plugins

asset-index: $(MANIFEST)
	@$(ASSETS) index

asset-update: asset-paths
	@$(ASSETS) clean $(if $(FORCE),--force) $(if $(PRUNE_EMPTY),--prune-empty)
	@$(ASSETS) tree $(if $(PLACEHOLDERS),--placeholders)
	@$(ASSETS) plugins
	@$(ASSETS) index

asset-serve:
	$(PYTHON) -m http.server $(PORT) --directory $(ASSET_ROOT)
