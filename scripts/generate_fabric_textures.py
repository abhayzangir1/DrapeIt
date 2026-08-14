import math
import os
import random
from PIL import Image, ImageFilter

OUTPUT_DIR = os.path.join(os.path.dirname(__file__), "..", "android", "app", "src", "main", "res", "drawable-nodpi")
os.makedirs(OUTPUT_DIR, exist_ok=True)
SIZE = 512

def create_image(pixels):
    img = Image.new("L", (SIZE, SIZE))
    scaled = [int(128 + ((p - 128) * 0.95)) for p in pixels]
    clamped = [max(10, min(245, s)) for s in scaled]
    img.putdata(clamped)
    return img

def noise2d(x, y, seed=42):
    n = math.sin(x * 12.9898 + y * 78.233 + seed) * 43758.5453
    return n - math.floor(n)

def perlin_seamless(x, y, freq=8, seed=42):
    fx = x / SIZE * math.pi * 2 * freq
    fy = y / SIZE * math.pi * 2 * freq
    v = (math.sin(fx) * math.cos(fy) + 
         math.sin(fx * 2 + seed) * math.cos(fy * 2) * 0.5 + 
         math.sin(fx * 4) * math.cos(fy * 4 + seed) * 0.25)
    return v / 1.75

print("Generating 14 photorealistic fabric bump maps...")

# 1. LEATHER (Cellular Voronoi grain + fine micro-creases)
def gen_leather():
    pixels = []
    points = []
    random.seed(101)
    num_cells = 140
    for _ in range(num_cells):
        points.append((random.randint(0, SIZE-1), random.randint(0, SIZE-1)))
    
    for y in range(SIZE):
        for x in range(SIZE):
            min_dist = 999.0
            second_dist = 999.0
            for px, py in points:
                dx = abs(x - px)
                dx = min(dx, SIZE - dx)
                dy = abs(y - py)
                dy = min(dy, SIZE - dy)
                dist = math.sqrt(dx*dx + dy*dy)
                if dist < min_dist:
                    second_dist = min_dist
                    min_dist = dist
                elif dist < second_dist:
                    second_dist = dist
            
            cell_edge = (second_dist - min_dist)
            crackle = perlin_seamless(x, y, freq=16, seed=12) * 25
            val = 128 + (cell_edge * 4.5) - crackle - 35
            pixels.append(val)
    img = create_image(pixels).filter(ImageFilter.GaussianBlur(radius=0.8))
    img.save(os.path.join(OUTPUT_DIR, "fabric_leather.png"))
    print("[OK] fabric_leather.png generated")

# 2. DENIM (45-degree 3x1 twill diagonal ribs + cross yarn)
def gen_denim():
    pixels = []
    for y in range(SIZE):
        for x in range(SIZE):
            diag = (x + y) % 12
            twill = 35 if diag < 6 else -35
            cross = math.sin(x * math.pi * 2 / 6) * 12 + math.sin(y * math.pi * 2 / 6) * 8
            grain = (noise2d(x % 64, y % 64, 5) - 0.5) * 30
            val = 128 + twill + cross + grain
            pixels.append(val)
    img = create_image(pixels).filter(ImageFilter.GaussianBlur(radius=0.5))
    img.save(os.path.join(OUTPUT_DIR, "fabric_denim.png"))
    print("[OK] fabric_denim.png generated")

# 3. CORDUROY (8-wale parallel vertical channels with soft velvet ridge texture)
def gen_corduroy():
    pixels = []
    wale_period = SIZE / 16.0
    for y in range(SIZE):
        for x in range(SIZE):
            phase = (x % wale_period) / wale_period
            ridge = math.sin(phase * math.pi) ** 0.8
            val_base = (ridge * 80) - 35
            nap = (noise2d(x % 32, y % 32, 9) - 0.5) * 20
            val = 128 + val_base + nap
            pixels.append(val)
    img = create_image(pixels).filter(ImageFilter.GaussianBlur(radius=0.6))
    img.save(os.path.join(OUTPUT_DIR, "fabric_corduroy.png"))
    print("[OK] fabric_corduroy.png generated")

# 4. TWEED (Herringbone zig-zag bouclé cross-hatching)
def gen_tweed():
    pixels = []
    col_width = SIZE / 8.0
    for y in range(SIZE):
        for x in range(SIZE):
            col = int(x / col_width)
            diag = (x + y if col % 2 == 0 else x - y) % 10
            stripe = 30 if diag < 5 else -30
            boucle = (noise2d(x % 16, y % 16, 7) - 0.5) * 45
            wool_fleck = 40 if (x * 7 + y * 13) % 97 == 0 else 0
            val = 128 + stripe + boucle + wool_fleck
            pixels.append(val)
    img = create_image(pixels).filter(ImageFilter.GaussianBlur(radius=0.7))
    img.save(os.path.join(OUTPUT_DIR, "fabric_tweed.png"))
    print("[OK] fabric_tweed.png generated")

# 5. LINEN (Irregular organic slub crosshatch)
def gen_linen():
    pixels = []
    for y in range(SIZE):
        for x in range(SIZE):
            slub_x = math.sin(x * math.pi * 2 / 10 + math.sin(y / 15.0) * 2.0) * 22
            slub_y = math.cos(y * math.pi * 2 / 12 + math.sin(x / 18.0) * 2.0) * 22
            organic = perlin_seamless(x, y, freq=12, seed=33) * 25
            val = 128 + slub_x + slub_y + organic
            pixels.append(val)
    img = create_image(pixels).filter(ImageFilter.GaussianBlur(radius=0.6))
    img.save(os.path.join(OUTPUT_DIR, "fabric_linen.png"))
    print("[OK] fabric_linen.png generated")

# 6. SILK (Fine micro-directional satin weave)
def gen_silk():
    pixels = []
    for y in range(SIZE):
        for x in range(SIZE):
            fine = math.sin(x * math.pi * 2 / 4) * 8 + math.cos(y * math.pi * 2 / 4) * 8
            sheen = math.sin((x + y * 0.5) * math.pi * 2 / 64) * 16
            val = 128 + fine + sheen
            pixels.append(val)
    img = create_image(pixels).filter(ImageFilter.GaussianBlur(radius=0.4))
    img.save(os.path.join(OUTPUT_DIR, "fabric_silk.png"))
    print("[OK] fabric_silk.png generated")

# 7. SATIN (High liquid gloss, ultra-smooth micro-filament)
def gen_satin():
    pixels = []
    for y in range(SIZE):
        for x in range(SIZE):
            fine = math.sin(x * math.pi * 2 / 3) * 5 + math.cos(y * math.pi * 2 / 3) * 5
            liquid = math.sin(x * math.pi * 2 / 128) * 12
            val = 128 + fine + liquid
            pixels.append(val)
    img = create_image(pixels).filter(ImageFilter.GaussianBlur(radius=0.3))
    img.save(os.path.join(OUTPUT_DIR, "fabric_satin.png"))
    print("[OK] fabric_satin.png generated")

# 8. VELVET (Dense cut-pile nap with light absorption)
def gen_velvet():
    pixels = []
    for y in range(SIZE):
        for x in range(SIZE):
            pile = (noise2d(x % 8, y % 8, 44) - 0.5) * 35
            cloud = perlin_seamless(x, y, freq=6, seed=88) * 30
            val = 128 + pile + cloud
            pixels.append(val)
    img = create_image(pixels).filter(ImageFilter.GaussianBlur(radius=0.8))
    img.save(os.path.join(OUTPUT_DIR, "fabric_velvet.png"))
    print("[OK] fabric_velvet.png generated")

# 9. CASHMERE (Ultra-fine cloud brushed fuzz)
def gen_cashmere():
    pixels = []
    for y in range(SIZE):
        for x in range(SIZE):
            fuzz = perlin_seamless(x, y, freq=24, seed=17) * 20
            soft = perlin_seamless(x, y, freq=4, seed=51) * 22
            val = 128 + fuzz + soft
            pixels.append(val)
    img = create_image(pixels).filter(ImageFilter.GaussianBlur(radius=1.0))
    img.save(os.path.join(OUTPUT_DIR, "fabric_cashmere.png"))
    print("[OK] fabric_cashmere.png generated")

# 10. WOOL (Worsted interlocking yarn knit)
def gen_wool():
    pixels = []
    for y in range(SIZE):
        for x in range(SIZE):
            yarn_x = math.sin(x * math.pi * 2 / 14) * 24
            yarn_y = math.sin(y * math.pi * 2 / 10 + math.sin(x * math.pi * 2 / 14) * 0.8) * 20
            grain = (noise2d(x % 16, y % 16, 21) - 0.5) * 15
            val = 128 + yarn_x + yarn_y + grain
            pixels.append(val)
    img = create_image(pixels).filter(ImageFilter.GaussianBlur(radius=0.7))
    img.save(os.path.join(OUTPUT_DIR, "fabric_wool.png"))
    print("[OK] fabric_wool.png generated")

# 11. CHIFFON (Featherlight sheer translucent grid)
def gen_chiffon():
    pixels = []
    for y in range(SIZE):
        for x in range(SIZE):
            grid_x = math.sin(x * math.pi * 2 / 8) ** 2 * 30 - 15
            grid_y = math.sin(y * math.pi * 2 / 8) ** 2 * 30 - 15
            val = 128 + grid_x + grid_y
            pixels.append(val)
    img = create_image(pixels).filter(ImageFilter.GaussianBlur(radius=0.5))
    img.save(os.path.join(OUTPUT_DIR, "fabric_chiffon.png"))
    print("[OK] fabric_chiffon.png generated")

# 12. KNIT (2x2 Ribbed vertical knit channels)
def gen_knit():
    pixels = []
    for y in range(SIZE):
        for x in range(SIZE):
            rib = math.sin(x * math.pi * 2 / 18) * 45
            loop = math.sin(y * math.pi * 2 / 8) * 15
            val = 128 + rib + loop
            pixels.append(val)
    img = create_image(pixels).filter(ImageFilter.GaussianBlur(radius=0.6))
    img.save(os.path.join(OUTPUT_DIR, "fabric_knit.png"))
    print("[OK] fabric_knit.png generated")

# 13. COTTON (Classic plain basketweave)
def gen_cotton():
    pixels = []
    for y in range(SIZE):
        for x in range(SIZE):
            weave = (math.sin(x * math.pi * 2 / 6) * math.cos(y * math.pi * 2 / 6)) * 26
            val = 128 + weave
            pixels.append(val)
    img = create_image(pixels).filter(ImageFilter.GaussianBlur(radius=0.5))
    img.save(os.path.join(OUTPUT_DIR, "fabric_cotton.png"))
    print("[OK] fabric_cotton.png generated")

# 14. POLYESTER (Technical micro-piqué honeycomb mesh)
def gen_polyester():
    pixels = []
    for y in range(SIZE):
        for x in range(SIZE):
            h1 = math.sin(x * math.pi * 2 / 10)
            h2 = math.sin((x * 0.5 + y * 0.866) * math.pi * 2 / 10)
            h3 = math.sin((x * 0.5 - y * 0.866) * math.pi * 2 / 10)
            mesh = (h1 * h2 * h3) * 35
            val = 128 + mesh
            pixels.append(val)
    img = create_image(pixels).filter(ImageFilter.GaussianBlur(radius=0.5))
    img.save(os.path.join(OUTPUT_DIR, "fabric_polyester.png"))
    print("[OK] fabric_polyester.png generated")

gen_leather()
gen_denim()
gen_corduroy()
gen_tweed()
gen_linen()
gen_silk()
gen_satin()
gen_velvet()
gen_cashmere()
gen_wool()
gen_chiffon()
gen_knit()
gen_cotton()
gen_polyester()

print("All 14 seamless fabric textures generated successfully!")
