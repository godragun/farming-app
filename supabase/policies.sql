-- Supabase Storage policies for bucket "media"
-- Safe to run multiple times (drops existing policies with the same name first).

-- Ensure RLS is enabled on storage.objects (it is by default, but be explicit).
alter table if exists storage.objects enable row level security;

-- Clean up any previous versions of these policies.
drop policy if exists "anon insert into media" on storage.objects;
drop policy if exists "read media objects"      on storage.objects;

-- Allow the anon (public) API key to upload new objects into the "media" bucket only.
create policy "anon insert into media"
on storage.objects
for insert
to anon
with check (bucket_id = 'media');

-- Allow anyone (anon + authenticated) to read objects in the "media" bucket.
-- Your bucket is already Public, but an explicit SELECT policy makes listing / API reads safe.
create policy "read media objects"
on storage.objects
for select
to anon, authenticated
using (bucket_id = 'media');

-- Sanity check: list the policies created.
select policyname, cmd, roles
from   pg_policies
where  schemaname = 'storage'
  and  tablename  = 'objects'
  and  policyname in ('anon insert into media', 'read media objects');
