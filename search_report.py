import re

with open('graphify-out/GRAPH_REPORT.md', 'r', encoding='utf-8') as f:
    content = f.read()

with open('graphify-out/report_search.txt', 'w', encoding='utf-8') as out:
    # 搜索Controller相关内容
    lines = content.split('\n')
    for i, line in enumerate(lines):
        if re.search(r'Controller|@RequestMapping|@GetMapping|@PostMapping|@PutMapping|@DeleteMapping', line, re.IGNORECASE):
            out.write('L{}: {}\n'.format(i+1, line))

print('Done')