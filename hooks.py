import re

MERMAID_BLOCK = re.compile(
    r'<pre class="mermaid"><code>(.*?)</code></pre>',
    re.DOTALL,
)


def on_page_content(html, page, config, files):
    return MERMAID_BLOCK.sub(r'<pre class="mermaid">\1</pre>', html)
