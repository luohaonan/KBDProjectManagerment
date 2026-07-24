import json

# 检查metadata.json
with open('graphify-out/metadata.json', 'r', encoding='utf-8') as f:
    meta = json.load(f)

print("=== metadata.json 结构 ===")
if isinstance(meta, dict):
    for k, v in meta.items():
        if isinstance(v, list):
            print(f"  {k}: list of {len(v)} items")
            if len(v) > 0:
                print(f"    first item keys: {list(v[0].keys()) if isinstance(v[0], dict) else type(v[0])}")
        elif isinstance(v, dict):
            print(f"  {k}: dict with {len(v)} keys")
        else:
            print(f"  {k}: {type(v).__name__} = {repr(v)[:100]}")
elif isinstance(meta, list):
    print(f"  list of {len(meta)} items")
    if len(meta) > 0:
        print(f"  first item: {repr(meta[0])[:200]}")