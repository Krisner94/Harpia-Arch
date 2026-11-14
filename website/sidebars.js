// @ts-check

/** @type {import('@docusaurus/plugin-content-docs').SidebarsConfig} */
const sidebars = {
  docs: [
    'index',
    'getting-started',
    'modules',
    {
      type: 'category',
      label: 'Núcleo',
      collapsed: false,
      items: [
        'core/workflow-engine',
        'core/context',
        'core/annotations',
        'core/build-validation',
        'core/errors',
      ],
    },
    {
      type: 'category',
      label: 'Guias',
      collapsed: false,
      items: [
        'guides/configuration',
        'guides/contract-first',
        'guides/persistence',
        'guides/security',
        'guides/native-image',
        'guides/testing',
        'guides/performance',
        'guides/benchmarks',
        'guides/maven-central',
      ],
    },
    {
      type: 'category',
      label: 'Arquitetura',
      collapsed: false,
      items: ['architecture/overview', 'architecture/decisions'],
    },
    {
      type: 'category',
      label: 'Referência',
      collapsed: false,
      items: [
        'reference/api',
        'reference/compatibility',
        'reference/glossary',
        'faq',
      ],
    },
    'contributing',
    'changelog',
  ],
};

module.exports = sidebars;
