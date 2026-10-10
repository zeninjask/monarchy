"""Rewrite items saved in the 1.20.5+ format ({count: int, id}) inside a structure .nbt to the 1.20.1 format
({Count: byte, id}), which 1.20.1 can read. Usage: fix_structure_items.py <in.nbt> <out.nbt>; prints the count."""
import gzip, struct, sys

def read(b, i, t):
    if t == 1: return b[i:i+1], i+1
    if t == 2: return b[i:i+2], i+2
    if t in (3, 5): return b[i:i+4], i+4
    if t in (4, 6): return b[i:i+8], i+8
    if t == 7: n = struct.unpack('>i', b[i:i+4])[0]; return b[i:i+4+n], i+4+n
    if t == 8: n = struct.unpack('>H', b[i:i+2])[0]; return b[i:i+2+n], i+2+n
    if t == 9:
        et = b[i]; n = struct.unpack('>i', b[i+1:i+5])[0]; j = i+5; items = []
        for _ in range(n): v, j = read(b, j, et); items.append(v)
        return ('list', et, items), j
    if t == 10:
        entries = []
        while True:
            tt = b[i]; i += 1
            if tt == 0: return ('compound', entries), i
            n = struct.unpack('>H', b[i:i+2])[0]; name = b[i+2:i+2+n].decode(); i += 2+n
            v, i = read(b, i, tt); entries.append([tt, name, v])
    if t == 11: n = struct.unpack('>i', b[i:i+4])[0]; return b[i:i+4+n*4], i+4+n*4
    if t == 12: n = struct.unpack('>i', b[i:i+4])[0]; return b[i:i+4+n*8], i+4+n*8
    raise ValueError(t)

def write(t, v):
    if t == 9:
        _, et, items = v
        return bytes([et]) + struct.pack('>i', len(items)) + b''.join(write(et, x) for x in items)
    if t == 10:
        out = b''
        for tt, name, x in v[1]:
            nb = name.encode(); out += bytes([tt]) + struct.pack('>H', len(nb)) + nb + write(tt, x)
        return out + b'\x00'
    return v

fixed = 0
def walk(t, v):
    global fixed
    if t == 9:
        for x in v[2]: walk(v[1], x)
    elif t == 10:
        names = {e[1] for e in v[1]}
        if 'id' in names and 'count' in names and 'Count' not in names:
            for e in v[1]:
                if e[1] == 'count' and e[0] == 3:
                    n = struct.unpack('>i', e[2])[0]
                    e[0], e[1], e[2] = 1, 'Count', struct.pack('>b', max(-128, min(127, n)))
                    fixed += 1
        for e in v[1]: walk(e[0], e[2])

data = gzip.decompress(open(sys.argv[1], 'rb').read())
assert data[0] == 10
n = struct.unpack('>H', data[1:3])[0]
root, end = read(data, 3+n, 10)
assert end == len(data)
walk(10, root)
open(sys.argv[2], 'wb').write(gzip.compress(data[:3+n] + write(10, root), mtime=0))
print(fixed)
