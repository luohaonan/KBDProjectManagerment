import json

with open('graphify-out/metadata.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

with open('graphify-out/metadata_check.txt', 'w', encoding='utf-8') as out:
    out.write('type: {}\n'.format(type(data).__name__))
    if isinstance(data, dict):
        out.write('keys: {}\n'.format(list(data.keys())))
        for k, v in list(data.items())[:5]:
            out.write('\n--- {} ---\n'.format(k))
            out.write('type: {}\n'.format(type(v).__name__))
            if isinstance(v, list):
                out.write('len: {}\n'.format(len(v)))
                if v:
                    out.write('first: {}\n'.format(json.dumps(v[0], ensure_ascii=False)[:500]))
            elif isinstance(v, dict):
                out.write('keys: {}\n'.format(list(v.keys())[:10]))
            else:
                out.write('value: {}\n'.format(str(v)[:200]))
    elif isinstance(data, list):
        out.write('len: {}\n'.format(len(data)))
        if data:
            out.write('first: {}\n'.format(json.dumps(data[0], ensure_ascii=False)[:1000]))

print('Done')