import type { MetadataRoute } from "next";
import { site } from "@/lib/site";
import { getCollectionSlugs } from "@/lib/data/collections";
import { fetchProducts } from "@/lib/api/products";

/**
 * Sitemap generator. Product slugs are pulled from the live backend so
 * the sitemap always reflects the true catalogue. If the backend is
 * unreachable at build time the product section is silently omitted
 * rather than failing the build — the store still works, sitemap is
 * simply regenerated on the next redeploy when the backend is up.
 */
export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const now = new Date();

  const staticRoutes: {
    path: string;
    priority: number;
    changeFrequency: MetadataRoute.Sitemap[number]["changeFrequency"];
  }[] = [
    { path: "", priority: 1.0, changeFrequency: "weekly" },
    { path: "/collections", priority: 0.9, changeFrequency: "weekly" },
    { path: "/shop", priority: 0.9, changeFrequency: "weekly" },
    { path: "/lookbook", priority: 0.8, changeFrequency: "monthly" },
    { path: "/about", priority: 0.7, changeFrequency: "monthly" },
    { path: "/contact", priority: 0.7, changeFrequency: "monthly" },
  ];

  const collectionRoutes = getCollectionSlugs().map((slug) => ({
    path: `/collections/${slug}`,
    priority: 0.8,
    changeFrequency: "monthly" as const,
  }));

  const productRoutes = await fetchProductSlugsSafe();

  return [...staticRoutes, ...collectionRoutes, ...productRoutes].map((r) => ({
    url: `${site.url}${r.path}`,
    lastModified: now,
    changeFrequency: r.changeFrequency,
    priority: r.priority,
  }));
}

/**
 * Pull every product slug via the backend list endpoint. Fetches in
 * pages of 100 (the backend's configured max) up to a safe cap so a
 * runaway catalogue can't blow up the build.
 */
async function fetchProductSlugsSafe(): Promise<
  { path: string; priority: number; changeFrequency: "weekly" }[]
> {
  const MAX_PAGES = 20; // 20 * 100 = 2000 products max in the sitemap
  const slugs: string[] = [];

  try {
    for (let page = 0; page < MAX_PAGES; page++) {
      const result = await fetchProducts({ page, size: 100 });
      for (const p of result.content) slugs.push(p.slug);
      if (result.last) break;
    }
  } catch (err) {
    console.warn(
      "[sitemap] Product slugs unavailable — backend unreachable at build.",
      err
    );
    return [];
  }

  return slugs.map((slug) => ({
    path: `/shop/${slug}`,
    priority: 0.7,
    changeFrequency: "weekly",
  }));
}
