# Asset housekeeping for the static assets server.
# The asset root is a plain folder: point python -m http.server / nginx / Tomcat at it.

PYTHON     ?= python3
ASSET_ROOT ?= data/cards/assets
ASSET_DEFS ?= data/cards/assets.defs.json
MANIFEST   ?= data/cards/assets.manifest
SRC        ?=
PORT       ?= 8080

ASSETS := $(PYTHON) dlp6/danlp6/assets.py --defs $(ASSET_DEFS) --root $(ASSET_ROOT) --manifest $(MANIFEST)

# Sources of truth the manifest is derived from
DEF_SOURCES := $(ASSET_DEFS) data/cards/CardMeta.json $(wildcard CardAPI/src/main/java/de/adrian/utils/*.java)

.PHONY: help asset-paths asset-list asset-tree asset-check asset-clean asset-gather asset-index asset-update asset-serve

help:
	@echo "asset-paths   derive expected asset paths -> $(MANIFEST)"
	@echo "asset-list    list files present under $(ASSET_ROOT)"
	@echo "asset-tree    create folder structure (PLACEHOLDERS=1 for empty files)"
	@echo "asset-check   diff expected vs. present (STRICT=1 fails on missing)"
	@echo "asset-clean   remove orphans/empty dirs (dry run; FORCE=1, PRUNE_EMPTY=1)"
	@echo "asset-gather  copy source assets in (SRC=<dir>)"
	@echo "asset-index   write $(ASSET_ROOT)/index.json with sizes + sha256"
	@echo "asset-update  paths -> clean -> tree -> index"
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

asset-index: $(MANIFEST)
	@$(ASSETS) index

asset-update: asset-paths
	@$(ASSETS) clean $(if $(FORCE),--force) $(if $(PRUNE_EMPTY),--prune-empty)
	@$(ASSETS) tree $(if $(PLACEHOLDERS),--placeholders)
	@$(ASSETS) index

asset-serve:
	$(PYTHON) -m http.server $(PORT) --directory $(ASSET_ROOT)
