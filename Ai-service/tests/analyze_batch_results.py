import json
from collections import defaultdict, Counter

import os
P=os.path.join(os.path.dirname(__file__),'batch_results.jsonl')

def load(path=P):
    res=[]
    with open(path,encoding='utf-8') as f:
        for line in f:
            if line.strip():
                res.append(json.loads(line))
    return res

def main():
    results = load()
    total=len(results)
    passed=sum(1 for r in results if r.get('passed'))
    failed=total-passed
    print(json.dumps({
        'total_tests': total,
        'passed': passed,
        'failed': failed,
        'intent_accuracy': round(passed/total if total else 0,3)
    }, ensure_ascii=False, indent=2))

    by_expected=defaultdict(lambda: {'total':0,'passed':0})
    for r in results:
        e=r.get('expected_intent')
        by_expected[e]['total']+=1
        if r.get('passed'):
            by_expected[e]['passed']+=1

    print('\nBreakdown by expected intent:')
    for k,v in by_expected.items():
        rate=v['passed']/v['total'] if v['total'] else 0
        print(f"- {k}: {v['passed']}/{v['total']} = {rate:.2f}")

    fails=[r for r in results if not r.get('passed')]
    pairs = Counter((r['expected_intent'], r['actual_intent']) for r in fails)
    print('\nMost common failures:')
    for (e,a),c in pairs.most_common():
        print(f"- expected {e} but got {a}: {c}")

    complaints=[r for r in results if r.get('actual_intent')=='complaint']
    if complaints:
        tp = sum(1 for r in complaints if r.get('expected_intent')=='complaint')
        print('\nComplaint precision:', tp, '/', len(complaints), '=', round(tp/len(complaints),3))

    print('\nFallback rate:', round(sum(1 for r in results if r.get('is_fallback'))/len(results),3))

if __name__=='__main__':
    main()
