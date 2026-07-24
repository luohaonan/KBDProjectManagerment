import json
import os
import hashlib

cache_dir = 'graphify-out/cache'

# 找一个Controller相关的cache文件
# ProjectController.java 的路径哈希
controller_path = 'D:\\KBDProjectManagerment\\backend\\src\\main\\java\\com\\kbd\\pms\\web\\ProjectController.java'
file_hash = hashlib.sha256(controller_path.encode('utf-8')).hexdigest()
print('Expected hash for ProjectController.java:', file_hash)

# 也试试相对路径
rel_path = 'backend/src/main/java/com/kbd/pms/web/ProjectController.java'
file_hash2 = hashlib.sha256(rel_path.encode('utf-8')).hexdigest()
print('Expected hash (rel path):', file_hash2)

# 检查第一个cache文件的结构
cache_files = os.listdir(cache_dir)
if cache_files:
    first_file = os.path.join(cache_dir, cache_files[0])
    with open(first_file, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    with open('graphify-out/cache_sample.txt', 'w', encoding='utf-8') as out:
        out.write('File: {}\n'.format(cache_files[0]))
        out.write('Type: {}\n'.format(type(data).__name__))
        if isinstance(data, dict):
            out.write('Keys: {}\n'.format(list(data.keys())))
            for k, v in list(data.items())[:5]:
                out.write('\n--- {} ---\n'.format(k))
                val_str = json.dumps(v, ensure_ascii=False)[:500]
                out.write(val_str)
        elif isinstance(data, list):
            out.write('Len: {}\n'.format(len(data)))
            if data:
                out.write('First: {}\n'.format(json.dumps(data[0], ensure_ascii=False)[:500]))

print('Done')