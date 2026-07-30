import { Routes } from '@angular/router';
import { adminGuard } from './core/guards/admin.guard';
import { authGuard } from './core/guards/auth.guard';
import { guestGuard } from './core/guards/guest.guard';

export const routes: Routes = [
  {
    path: '',
    title: 'BidForge — Auction Engine',
    loadComponent: () => import('./features/home/home').then((m) => m.Home),
  },

  {
    path: 'auctions',
    title: 'Browse auctions · BidForge',
    loadComponent: () => import('./features/auctions/browse/browse').then((m) => m.Browse),
  },
  {
    path: 'auctions/new',
    title: 'New auction · BidForge',
    canActivate: [authGuard],
    loadComponent: () => import('./features/auctions/form/auction-form').then((m) => m.AuctionForm),
  },
  {
    path: 'auctions/:id/edit',
    title: 'Edit auction · BidForge',
    canActivate: [authGuard],
    loadComponent: () => import('./features/auctions/form/auction-form').then((m) => m.AuctionForm),
  },
  {
    path: 'auctions/:id',
    title: 'Auction · BidForge',
    loadComponent: () => import('./features/auctions/detail/detail').then((m) => m.AuctionDetail),
  },

  {
    path: 'my/auctions',
    title: 'My auctions · BidForge',
    canActivate: [authGuard],
    loadComponent: () => import('./features/my/my-auctions/my-auctions').then((m) => m.MyAuctions),
  },
  {
    path: 'my/bids',
    title: 'My bids · BidForge',
    canActivate: [authGuard],
    loadComponent: () => import('./features/my/my-bids/my-bids').then((m) => m.MyBids),
  },
  {
    path: 'my/won',
    title: 'Auctions I won · BidForge',
    canActivate: [authGuard],
    loadComponent: () => import('./features/my/my-won/my-won').then((m) => m.MyWon),
  },
  {
    path: 'my/profile',
    title: 'My profile · BidForge',
    canActivate: [authGuard],
    loadComponent: () => import('./features/my/my-profile/my-profile').then((m) => m.MyProfile),
  },

  {
    path: 'admin/users',
    title: 'Users · BidForge admin',
    canActivate: [adminGuard],
    loadComponent: () => import('./features/admin/users/admin-users').then((m) => m.AdminUsers),
  },
  {
    path: 'admin/auctions',
    title: 'All auctions · BidForge admin',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./features/admin/auctions/admin-auctions').then((m) => m.AdminAuctions),
  },
  {
    path: 'admin/audit',
    title: 'Audit trail · BidForge admin',
    canActivate: [adminGuard],
    loadComponent: () => import('./features/admin/audit/admin-audit').then((m) => m.AdminAudit),
  },

  {
    path: 'login',
    title: 'Sign in · BidForge',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login/login').then((m) => m.Login),
  },
  {
    path: 'register',
    title: 'Create account · BidForge',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/register/register').then((m) => m.Register),
  },

  {
    path: 'forbidden',
    title: 'Access denied · BidForge',
    loadComponent: () => import('./features/errors/forbidden/forbidden').then((m) => m.Forbidden),
  },
  {
    path: '**',
    title: 'Page not found · BidForge',
    loadComponent: () => import('./features/errors/not-found/not-found').then((m) => m.NotFound),
  },
];
