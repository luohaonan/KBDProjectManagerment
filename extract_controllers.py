import json

with open('graphify-out/graph.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

nodes = data['nodes']

with open('graphify-out/controller_analysis.txt', 'w', encoding='utf-8') as out:
    out.write('Total nodes: {}\n'.format(len(nodes)))
    
    # 先看前10个节点的所有字段和值
    out.write('\n=== First 10 nodes ===\n')
    for i, n in enumerate(nodes[:10]):
        out.write('Node {}:\n'.format(i))
        for k, v in n.items():
            val_str = str(v)[:300]
            out.write('  {}: {}\n'.format(k, val_str))
        out.write('\n')
    
    # 搜索所有字段中包含Controller的节点
    out.write('\n=== Nodes containing "Controller" in any field ===\n')
    count = 0
    for n in nodes:
        node_str = json.dumps(n, ensure_ascii=False)
        if 'Controller' in node_str:
            count += 1
            out.write('Node id={}, label={}\n'.format(n.get('id',''), n.get('label','')))
            for k, v in n.items():
                if k not in ('id', 'label'):
                    val_str = str(v)[:200]
                    out.write('  {}: {}\n'.format(k, val_str))
            out.write('\n')
    out.write('Total Controller-related nodes: {}\n'.format(count))

print('Done')