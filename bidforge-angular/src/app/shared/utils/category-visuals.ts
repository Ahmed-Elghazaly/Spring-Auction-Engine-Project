import { AuctionCategory } from '../../core/models/enums';

export interface CategoryVisual {
  icon: string;

  color: string;
}

export const CATEGORY_VISUALS: Record<AuctionCategory, CategoryVisual> = {
  ELECTRONICS: { icon: 'devices', color: '#1e88e5' },
  VEHICLES: { icon: 'directions_car', color: '#ef6c00' },
  ART: { icon: 'palette', color: '#8e24aa' },
  COLLECTIBLES: { icon: 'diamond', color: '#00897b' },
  FASHION: { icon: 'checkroom', color: '#d81b60' },
  BOOKS: { icon: 'menu_book', color: '#6d4c41' },
  SPORTS: { icon: 'sports_soccer', color: '#43a047' },
  OTHER: { icon: 'category', color: '#546e7a' },
};

export function categoryVisual(category: AuctionCategory): CategoryVisual {
  return CATEGORY_VISUALS[category] ?? CATEGORY_VISUALS.OTHER;
}
