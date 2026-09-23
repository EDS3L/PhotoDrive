import type { Block, Inline } from '../types';

function InlineText({ content }: { content: Inline[] }) {
	return (
		<>
			{content.map((c, i) =>
				typeof c === 'string' ? (
					c
				) : (
					<strong key={i} className='font-semibold text-foreground'>
						{c.b}
					</strong>
				),
			)}
		</>
	);
}

export function AnswerBlocks({ blocks }: { blocks: Block[] }) {
	return (
		<div className='space-y-3 text-[15px] leading-relaxed text-foreground/80'>
			{blocks.map((block, i) => {
				switch (block.k) {
					case 'h3':
						return (
							<h4
								key={i}
								className='pt-4 font-serif text-xl text-accent'
							>
								<InlineText content={block.c} />
							</h4>
						);
					case 'li':
					case 'li2':
						return (
							<p
								key={i}
								className={
									block.k === 'li'
										? 'relative pl-5 before:absolute before:left-1 before:content-["•"] before:text-accent'
										: 'relative pl-10 before:absolute before:left-6 before:content-["◦"] before:text-muted'
								}
							>
								<InlineText content={block.c} />
							</p>
						);
					case 'code':
						return (
							<pre
								key={i}
								className='overflow-x-auto bg-background border border-border p-4 text-xs font-mono text-foreground/90'
							>
								<InlineText content={block.c} />
							</pre>
						);
					case 'table':
						return (
							<div key={i} className='overflow-x-auto'>
								<table className='w-full border-collapse text-sm'>
									<tbody>
										{block.rows.map((row, r) => (
											<tr key={r} className='border-b border-border'>
												{row.map((cell, c) =>
													r === 0 ? (
														<th
															key={c}
															className='px-3 py-2 text-left font-medium text-foreground'
														>
															{cell}
														</th>
													) : (
														<td key={c} className='px-3 py-2 align-top'>
															{cell}
														</td>
													),
												)}
											</tr>
										))}
									</tbody>
								</table>
							</div>
						);
					default:
						return (
							<p key={i}>
								<InlineText content={block.c} />
							</p>
						);
				}
			})}
		</div>
	);
}
